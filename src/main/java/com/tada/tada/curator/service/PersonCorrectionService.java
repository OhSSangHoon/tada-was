package com.tada.tada.curator.service;

import com.tada.tada.curator.dto.PersonCorrectionForm;
import com.tada.tada.curator.entity.MemoryPerson;
import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.entity.MentionCandidateStatus;
import com.tada.tada.curator.entity.MentionEntityType;
import com.tada.tada.curator.repository.MemoryPersonRepository;
import com.tada.tada.curator.repository.MentionCandidateRepository;
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
	private final DiaryRepository diaryRepository;
	private final DiaryPersonService diaryPersonService;
	private final PersonAggregateService personAggregateService;
	private final PersonAliasService personAliasService;

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
		 * 앞선 조회는 요청 검증과 대상 확정을 위한 것이고,
		 * 여기서는 Alias 저장 전 동시성 제어를 위해 Person을 잠근 뒤
		 * lock 시점의 존재·소유권을 다시 확인한다.
		 *
		 * 관련 Person은 UUID 순서로 잠가 Alias INSERT 경쟁과
		 * lock 순서 불일치로 인한 deadlock 가능성을 줄인다.
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

		personAliasService.saveIfAbsent(
				userId,
				targetPersonId,
				candidate.getRawText()
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

		String displayName =
				form.getNewDisplayName()
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
}
