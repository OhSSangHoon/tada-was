package com.tada.tada.curator.service;

import com.tada.tada.curator.dto.PersonCorrectionForm;
import com.tada.tada.curator.entity.MemoryPerson;
import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.entity.MentionCandidateStatus;
import com.tada.tada.curator.entity.MentionEntityType;
import com.tada.tada.curator.entity.PersonAlias;
import com.tada.tada.curator.model.PersonNormalization;
import com.tada.tada.curator.repository.MemoryPersonRepository;
import com.tada.tada.curator.repository.MentionCandidateRepository;
import com.tada.tada.curator.repository.PersonAliasRepository;
import com.tada.tada.diary.entity.Diary;
import com.tada.tada.diary.repository.DiaryRepository;
import com.tada.tada.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonCorrectionService {

	private final MentionCandidateRepository mentionCandidateRepository;
	private final MemoryPersonRepository memoryPersonRepository;
	private final PersonAliasRepository personAliasRepository;
	private final DiaryRepository diaryRepository;
	private final DiaryPersonService diaryPersonService;
	private final PersonAggregateService personAggregateService;
	private final PersonNormalizer personNormalizer;

	@Transactional
	public void correctPerson(
			UUID userId,
			UUID currentPersonId,
			UUID candidateId,
			PersonCorrectionForm form
	) {
		if (userId == null
				|| currentPersonId == null
				|| candidateId == null
				|| form == null) {

			throw new CustomException(
					"잘못된 교정 요청입니다.",
					400
			);
		}

		validateCorrectionTarget(form);

		UUID diaryId =
				mentionCandidateRepository
						.findDiaryIdById(candidateId)
						.orElseThrow(
								() -> new CustomException(
										"대상 기록을 찾을 수 없습니다.",
										404
								)
						);

		Diary diary =
				diaryRepository
						.findByIdForUpdate(diaryId)
						.orElseThrow(
								() -> new CustomException(
										"대상 기록을 찾을 수 없습니다.",
										404
								)
						);

		if (!userId.equals(diary.getUserId())) {
			throw new CustomException(
					"대상 기록을 찾을 수 없습니다.",
					404
			);
		}

		if (!diary.isActive()) {
			throw new CustomException(
					"활성 상태의 일기에서만 인물을 교정할 수 있습니다.",
					400
			);
		}

		MentionCandidate candidate =
				mentionCandidateRepository
						.findByIdForUpdate(candidateId)
						.orElseThrow(
								() -> new CustomException(
										"대상 기록을 찾을 수 없습니다.",
										404
								)
						);

		validateCandidate(
				candidate,
				diaryId
		);

		UUID oldPersonId =
				candidate.getMatchedPersonId();

		if (!currentPersonId.equals(oldPersonId)) {
			throw new CustomException(
					"이미 변경된 인물 정보입니다.",
					409
			);
		}

		requireOwnedPerson(
				userId,
				oldPersonId
		);

		MemoryPerson targetPerson =
				resolveTargetPerson(
						userId,
						form
				);

		UUID targetPersonId =
				targetPerson.getId();

		if (oldPersonId.equals(targetPersonId)) {
			throw new CustomException(
					"현재 인물과 다른 인물을 선택해주세요.",
					400
			);
		}

		/*
		 * Alias 존재 확인/저장 전에 관련 기존 Person들을 동일한 UUID 순서로 잠근다.
		 *
		 * 서로 다른 Diary에서 같은 Person으로 동시에 교정될 경우
		 * 두 요청이 모두 Alias exists=false를 본 뒤 INSERT하여
		 * UNIQUE 제약 위반으로 한 요청이 실패하는 경쟁을 막는다.
		 *
		 * PersonAggregateService도 UUID 오름차순으로 Person을 잠그므로
		 * 동일한 순서를 유지해 deadlock 가능성을 낮춘다.
		 *
		 * 새 Person은 현재 트랜잭션에서 생성되어 다른 트랜잭션이 아직 참조할 수 없으므로
		 * 기존 target을 선택한 경우에만 target까지 잠근다.
		 */
		List<UUID> personIdsToLock =
				new ArrayList<>();

		personIdsToLock.add(oldPersonId);

		if (form.getTargetPersonId() != null) {
			personIdsToLock.add(targetPersonId);
		}

		lockOwnedPersonsInOrder(
				userId,
				personIdsToLock
		);

		candidate.confirmPerson(
				targetPersonId
		);

		saveAliasIfAbsent(
				userId,
				targetPersonId,
				candidate
		);

		List<MentionCandidate> personCandidates =
				mentionCandidateRepository
						.findAllByDiaryId(diaryId)
						.stream()
						.filter(
								item ->
										item.getEntityType()
												== MentionEntityType.PERSON
						)
						.filter(
								item ->
										item.getStatus()
												== MentionCandidateStatus.CONFIRMED
						)
						.filter(
								item ->
										item.getMatchedPersonId() != null
						)
						.toList();

		Set<UUID> affectedPersonIds =
				new HashSet<>(
						diaryPersonService
								.reconcileDiaryPersons(
										diaryId,
										userId,
										personCandidates
								)
				);

		affectedPersonIds.add(
				oldPersonId
		);

		affectedPersonIds.add(
				targetPersonId
		);

		personAggregateService.recalculate(
				userId,
				affectedPersonIds
		);
	}

	private void validateCorrectionTarget(
			PersonCorrectionForm form
	) {
		boolean hasTargetPerson =
				form.getTargetPersonId() != null;

		boolean hasNewDisplayName =
				form.getNewDisplayName() != null;

		if (hasTargetPerson == hasNewDisplayName) {
			throw new CustomException(
					"기존 인물 또는 새 인물 중 하나만 선택해주세요.",
					400
			);
		}

		if (hasNewDisplayName
				&& form.getNewDisplayName().isBlank()) {

			throw new CustomException(
					"새 인물 이름을 입력해주세요.",
					400
			);
		}
	}

	private void validateCandidate(
			MentionCandidate candidate,
			UUID diaryId
	) {
		if (!diaryId.equals(
				candidate.getDiaryId()
		)) {
			throw new CustomException(
					"대상 기록이 올바르지 않습니다.",
					400
			);
		}

		if (candidate.getEntityType()
				!= MentionEntityType.PERSON) {

			throw new CustomException(
					"PERSON Candidate만 교정할 수 있습니다.",
					400
			);
		}

		if (candidate.getStatus()
				!= MentionCandidateStatus.CONFIRMED
				|| candidate.getMatchedPersonId() == null) {

			throw new CustomException(
					"현재 교정할 수 없는 Candidate입니다.",
					400
			);
		}
	}

	private MemoryPerson resolveTargetPerson(
			UUID userId,
			PersonCorrectionForm form
	) {
		if (form.getTargetPersonId() != null) {
			return requireOwnedPerson(
					userId,
					form.getTargetPersonId()
			);
		}

		String requestedName =
				form.getNewDisplayName().strip();

		PersonNormalization normalization =
				personNormalizer.normalize(
						requestedName
				);

		String displayName =
				normalization
						.displayNameCandidate()
						.strip();

		if (displayName.isBlank()) {
			throw new CustomException(
					"새 인물 이름을 입력해주세요.",
					400
			);
		}

		MemoryPerson newPerson =
				MemoryPerson.create(
						userId,
						displayName
				);

		return memoryPersonRepository.save(
				newPerson
		);
	}

	private MemoryPerson requireOwnedPerson(
			UUID userId,
			UUID personId
	) {
		return memoryPersonRepository
				.findByIdAndUserId(
						personId,
						userId
				)
				.orElseThrow(
						() -> new CustomException(
								"인물을 찾을 수 없습니다.",
								404
						)
				);
	}

	private void lockOwnedPersonsInOrder(
			UUID userId,
			Collection<UUID> personIds
	) {
		List<UUID> orderedPersonIds =
				new ArrayList<>(
						new HashSet<>(personIds)
				);

		Collections.sort(orderedPersonIds);

		for (UUID personId : orderedPersonIds) {
			MemoryPerson person =
					memoryPersonRepository
							.findByIdForUpdate(personId)
							.orElseThrow(
									() -> new CustomException(
											"인물을 찾을 수 없습니다.",
											404
									)
							);

			if (!userId.equals(person.getUserId())) {
				throw new CustomException(
						"인물을 찾을 수 없습니다.",
						404
				);
			}
		}
	}

	private void saveAliasIfAbsent(
			UUID userId,
			UUID targetPersonId,
			MentionCandidate candidate
	) {
		String aliasText =
				candidate
						.getRawText()
						.strip();

		if (aliasText.isBlank()) {
			return;
		}

		boolean exists =
				personAliasRepository
						.existsByOwnerUserIdAndPersonIdAndAliasText(
								userId,
								targetPersonId,
								aliasText
						);

		if (exists) {
			return;
		}

		String normalizedText =
				personNormalizer
						.normalizeName(
								aliasText
						);

		if (normalizedText.isBlank()) {
			return;
		}

		PersonAlias alias =
				PersonAlias.create(
						targetPersonId,
						userId,
						aliasText,
						normalizedText
				);

		personAliasRepository.save(alias);
	}
}