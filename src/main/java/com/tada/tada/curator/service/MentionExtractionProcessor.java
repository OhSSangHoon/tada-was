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
 * Diary 트랜잭션과 분리된 별도 트랜잭션에서 실행한다.
 * 여기서 실패하면 이 트랜잭션만 rollback 되고 Diary 저장은 유지된다.
 *
 * 중복 Event 방어는 Diary row lock 을 먼저 잡고
 * 기존 Candidate 존재 여부를 확인하는 순서로 한다.
 * (Listener 가 AFTER_COMMIT 이므로 Diary 는 이미 commit 된 상태다)
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

	@Transactional(
			propagation = Propagation.REQUIRES_NEW
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
		 * 같은 extraction 안에서
		 * 조사/표현 차이만 있는 동일 PERSON ref가
		 * 이미 할당한 인물을 다시 사용할 수 있도록 한다.
		 *
		 * 예:
		 * p1 = "민수"
		 * p2 = "민수가"
		 *
		 * 둘 모두 normalizedText가 "민수"이면
		 * 이전 민수 personId 하나만 block에서 해제한다.
		 *
		 * 단, 같은 normalizedText에 이미 2명 이상이 할당된 경우에는
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

			Set<UUID> sameNormalizedPersonIds =
					assignedPersonIdsByNormalizedText
							.get(
									normalizedText
							);

			if (sameNormalizedPersonIds != null
					&& sameNormalizedPersonIds.size()
					== 1) {

				UUID reusablePersonId =
						sameNormalizedPersonIds
								.iterator()
								.next();

				blockedPersonIds.remove(
						reusablePersonId
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