package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.entity.MentionEntityType;
import com.tada.tada.curator.model.PersonNormalization;
import com.tada.tada.curator.validation.ExtractionResultValidator;
import com.tada.tada.diary.entity.Diary;
import com.tada.tada.diary.repository.DiaryRepository;
import com.tada.tada.global.event.MentionExtractedEvent;
import com.tada.tada.global.event.dto.ActivityExtraction;
import com.tada.tada.global.event.dto.ExtractionResult;
import com.tada.tada.global.event.dto.PersonExtraction;
import com.tada.tada.global.event.dto.PlaceExtraction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/*
 * Curator 후처리 본체.
 *
 * 일기 최종 저장 @Transactional 안에서 동기로 실행된다.
 * 별도 트랜잭션을 열지 않고 상위 저장 트랜잭션에 그대로 참여한다.
 * 여기서 실패하면 Diary, Sticker, Candidate, Relation, DiaryPerson,
 * PersonAggregate 를 포함한 저장 트랜잭션 전체가 rollback 된다.
 *
 * REQUIRES_NEW 를 쓰면 안 된다. 별도 트랜잭션이 되면
 * Curator 가 실패해도 Diary 만 commit 되어 정책이 깨진다.
 *
 * Diary row lock 과 기존 Candidate 확인은 남겨 두되,
 * 단일 트랜잭션 구조에서는 신규 저장 경로에서 실제로 걸리지 않는다.
 * 신규 저장은 diaryId 가 매번 새로 생성되어 동시 경쟁이 없고,
 * 새 Diary 라 hasCandidates 는 항상 false 이기 때문이다.
 * 이 방어가 실제로 필요해지는 시점은 본문 수정 재추출이다. (명세 12)
 *
 * 범위 한정: 이 방식은 "최초 MentionExtractedEvent 의 중복 수신" 만 막는다.
 * 이미 Candidate 가 있는 Diary 는 새 ExtractionResult 가 와도 skip 되므로,
 * 본문 수정 후 재추출까지 포함한 완전한 멱등성은 아니다.
 * 수정 Event 는 Candidate / Relation 의 KEEP·ADD·REMOVE reconcile 구조가
 * 별도로 필요하다. (명세 12)
 */
@Service
@RequiredArgsConstructor
public class MentionExtractionProcessor {

	private final DiaryRepository diaryRepository;
	private final ExtractionResultValidator extractionResultValidator;
	private final MentionCandidateService mentionCandidateService;
	private final MentionCandidatePersonRefService relationService;
	private final DiaryPersonService diaryPersonService;
	private final PersonAggregateService personAggregateService;
	private final PersonNormalizer personNormalizer;

	/*
	 * MANDATORY 다. 상위 트랜잭션이 없으면 즉시 예외가 난다.
	 *
	 * REQUIRED 였다면 트랜잭션 밖에서 이벤트가 발행됐을 때
	 * 여기서 새 트랜잭션을 열어 Curator 데이터만 따로 commit 된다.
	 * Diary 는 저장 안 됐는데 인물 데이터만 남는 상태가 되므로
	 * "트랜잭션 밖에서 발행 금지" 를 코드로 강제한다. (명세 17.1)
	 */
	@Transactional(
			propagation = Propagation.MANDATORY
	)
	public void process(
			MentionExtractedEvent event
	) {
		validateEvent(event);

		Diary diary =
				findAndValidateDiary(
						event.diaryId(),
						event.userId()
				);

		/*
		 * Diary row lock 이후에 확인해야 한다.
		 * lock 전에 검사하면 동시에 들어온 두 Event 가 모두 통과할 수 있다.
		 *
		 * 최초 처리 중복만 막는다. 본문 수정 재추출은 reconcile 이 필요하다.
		 */
		if (mentionCandidateService
				.hasCandidates(
						event.diaryId()
				)) {
			return;
		}

		ExtractionResult extractionResult =
				event.extractionResult();

		extractionResultValidator.validate(
				diary.getContent(),
				extractionResult
		);

		Map<String, MentionCandidate>
				personCandidatesByRef =
				createPersonCandidates(
						event.diaryId(),
						event.userId(),
						extractionResult.persons()
				);

		List<MentionCandidate> personCandidates =
				new ArrayList<>(
						personCandidatesByRef.values()
				);

		createPlaceCandidates(
				event.diaryId(),
				extractionResult.places(),
				personCandidatesByRef
		);

		createActivityCandidates(
				event.diaryId(),
				extractionResult.activities(),
				personCandidatesByRef
		);

		Set<UUID> affectedPersonIds =
				diaryPersonService
						.reconcileDiaryPersons(
								event.diaryId(),
								event.userId(),
								personCandidates
						);

		personAggregateService.recalculate(
				event.userId(),
				affectedPersonIds
		);
	}

	private Map<String, MentionCandidate>
	createPersonCandidates(
			UUID diaryId,
			UUID userId,
			List<PersonExtraction> persons
	) {
		/*
		 * AI 배열 순서를 그대로 유지한다. (명세 10.1-7)
		 * HashMap 이면 values() 순서가 비결정적이 된다.
		 */
		Map<String, MentionCandidate>
				candidatesByRef =
				new LinkedHashMap<>();

		Set<UUID> assignedPersonIds =
				new HashSet<>();

		/*
		 * 같은 extraction 안에서 이미 할당한 인물을 다시 사용할 수
		 * 있게 하는 두 가지 근거를 모은다. (findReusablePersonIdsInDiary)
		 *
		 *   1. 조사 변형: normalizedText가 완전히 같음
		 *      예: p1 = "민수", p2 = "민수가" (둘 다 "민수")
		 *   2. 성 포함/생략 변형: 한쪽에서 성을 떼면 다른 쪽과 같음
		 *      예: p1 = "민수", p2 = "김민수"
		 *      PersonMatchingService.matchesSurnameVariant 가 일기 간
		 *      유사도 점수에 쓰는 것과 같은 관계를, 같은 일기 안에서는
		 *      재사용 근거로 쓴다.
		 *
		 * 두 근거를 합친 후보가 정확히 한 명일 때만 block에서
		 * 해제한다. 이미 2명 이상이 걸리면(예: 이 일기에 "민수"와
		 * "김민수"가 서로 다른 사람으로 이미 따로 배정돼 있는 경우)
		 * 어떤 인물을 재사용해야 할지 확정할 수 없으므로
		 * 자동으로 unblock하지 않는다.
		 */
		Map<String, Set<UUID>>
				assignedPersonIdsByNormalizedText =
				new HashMap<>();

		for (PersonExtraction person : persons) {
			PersonNormalization normalization =
					personNormalizer.normalize(
							person.rawText()
					);

			String normalizedText =
					normalization.normalizedText();

			Set<UUID> blockedPersonIds =
					new HashSet<>(
							assignedPersonIds
					);

			Set<UUID> reusablePersonIds =
					findReusablePersonIdsInDiary(
							normalizedText,
							assignedPersonIdsByNormalizedText
					);

			if (reusablePersonIds.size() == 1) {
				blockedPersonIds.remove(
						reusablePersonIds
								.iterator()
								.next()
				);
			}

			MentionCandidate candidate =
					mentionCandidateService
							.createPersonCandidate(
									diaryId,
									userId,
									person.rawText(),
									blockedPersonIds
							);

			candidatesByRef.put(
					person.ref(),
					candidate
			);

			UUID matchedPersonId =
					candidate
							.getMatchedPersonId();

			assignedPersonIds.add(
					matchedPersonId
			);

			assignedPersonIdsByNormalizedText
					.computeIfAbsent(
							normalizedText,
							key -> new HashSet<>()
					)
					.add(
							matchedPersonId
					);
		}

		return candidatesByRef;
	}

	/*
	 * assignedPersonIdsByNormalizedText 에 쌓인, 이미 이 일기에서
	 * 배정된 (normalizedText -> personId) 관계 중에서 이번 ref 와
	 * 같은 사람일 근거가 있는 personId 를 모은다.
	 *
	 * 조사 변형(문자열이 같음)과 성 포함/생략 변형
	 * (personNormalizer.removeSurname 으로 서로 도달 가능함)
	 * 두 근거를 하나의 후보 집합으로 합친다.
	 *
	 * 호출부는 이 결과가 정확히 한 명일 때만 재사용한다. 두 근거가
	 * 서로 다른 사람을 가리키면 후보가 2명 이상이 되어 자동으로
	 * 보수적인 쪽(차단 유지)으로 떨어진다.
	 */
	private Set<UUID> findReusablePersonIdsInDiary(
			String normalizedText,
			Map<String, Set<UUID>>
					assignedPersonIdsByNormalizedText
	) {
		Set<UUID> reusablePersonIds =
				new HashSet<>();

		String surnameRemoved =
				personNormalizer.removeSurname(
						normalizedText
				);

		for (Map.Entry<String, Set<UUID>> entry
				: assignedPersonIdsByNormalizedText
				.entrySet()) {

			String assignedNormalizedText =
					entry.getKey();

			boolean sameParticleVariant =
					assignedNormalizedText.equals(
							normalizedText
					);

			boolean sameSurnameVariant =
					!sameParticleVariant
							&& (assignedNormalizedText
							.equals(surnameRemoved)

							|| personNormalizer
							.removeSurname(
									assignedNormalizedText
							)
							.equals(normalizedText));

			if (sameParticleVariant
					|| sameSurnameVariant) {

				reusablePersonIds.addAll(
						entry.getValue()
				);
			}
		}

		return reusablePersonIds;
	}

	private void createPlaceCandidates(
			UUID diaryId,
			List<PlaceExtraction> places,
			Map<String, MentionCandidate>
					personCandidatesByRef
	) {
		for (PlaceExtraction place : places) {
			MentionCandidate sourceCandidate =
					mentionCandidateService
							.createNonPersonCandidate(
									diaryId,
									place.rawText(),
									place.normalizedText(),
									MentionEntityType.PLACE
							);

			List<MentionCandidate> relatedPersons =
					resolvePersonCandidates(
							place.personRefs(),
							personCandidatesByRef
					);

			relationService.createRelations(
					diaryId,
					sourceCandidate,
					relatedPersons
			);
		}
	}

	private void createActivityCandidates(
			UUID diaryId,
			List<ActivityExtraction> activities,
			Map<String, MentionCandidate>
					personCandidatesByRef
	) {
		for (ActivityExtraction activity
				: activities) {

			MentionCandidate sourceCandidate =
					mentionCandidateService
							.createNonPersonCandidate(
									diaryId,
									activity.rawText(),
									activity.normalizedText(),
									MentionEntityType.ACTIVITY
							);

			List<MentionCandidate> relatedPersons =
					resolvePersonCandidates(
							activity.personRefs(),
							personCandidatesByRef
					);

			relationService.createRelations(
					diaryId,
					sourceCandidate,
					relatedPersons
			);
		}
	}

	private List<MentionCandidate>
	resolvePersonCandidates(
			List<String> personRefs,
			Map<String, MentionCandidate>
					personCandidatesByRef
	) {
		List<MentionCandidate> persons =
				new ArrayList<>();

		for (String personRef : personRefs) {
			MentionCandidate candidate =
					personCandidatesByRef.get(
							personRef
					);

			if (candidate == null) {
				throw new IllegalStateException(
						"PERSON candidate does not exist for ref: "
								+ personRef
				);
			}

			persons.add(candidate);
		}

		return persons;
	}

	private Diary findAndValidateDiary(
			UUID diaryId,
			UUID userId
	) {
		Diary diary =
				diaryRepository
						.findByIdForUpdate(
								diaryId
						)
						.orElseThrow(
								() ->
										new IllegalStateException(
												"diary does not exist"
										)
						);

		if (!userId.equals(
				diary.getUserId()
		)) {
			throw new IllegalStateException(
					"diary belongs to another user"
			);
		}

		if (!diary.isActive()) {
			throw new IllegalStateException(
					"diary must be active"
			);
		}

		return diary;
	}

	private void validateEvent(
			MentionExtractedEvent event
	) {
		if (event == null) {
			throw new IllegalArgumentException(
					"event must not be null"
			);
		}

		if (event.diaryId() == null) {
			throw new IllegalArgumentException(
					"diaryId must not be null"
			);
		}

		if (event.userId() == null) {
			throw new IllegalArgumentException(
					"userId must not be null"
			);
		}

		if (event.extractionResult() == null) {
			throw new IllegalArgumentException(
					"extractionResult must not be null"
			);
		}
	}
}