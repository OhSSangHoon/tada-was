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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

		ExtractionResult extractionResult =
				event.extractionResult();

		extractionResultValidator.validate(
				diary.getContent(),
				extractionResult
		);

		/*
		 * 신규 저장과 본문 수정을 같은 경로에서 처리한다.
		 *
		 * 신규 저장:
		 * existingCandidates == []
		 * -> 전부 ADD
		 *
		 * 동일 Event 재처리:
		 * -> 전부 KEEP
		 *
		 * 본문 수정:
		 * -> KEEP / ADD / REMOVE
		 */
		List<MentionCandidate> existingCandidates =
				mentionCandidateService
						.findAllByDiaryId(
								event.diaryId()
						);

		Map<String, List<MentionCandidate>>
				existingPersonCandidates =
				buildPersonCandidatePool(
						existingCandidates
				);

		Map<String, List<MentionCandidate>>
				existingSourceCandidates =
				buildSourceCandidatePool(
						existingCandidates
				);

		/*
		 * PERSON을 반드시 먼저 reconcile한다.
		 * PLACE / ACTIVITY의 personRefs를
		 * 실제 PERSON Candidate UUID로 바꿔야 하기 때문이다.
		 */
		Map<String, MentionCandidate>
				personCandidatesByRef =
				reconcilePersonCandidates(
						event.diaryId(),
						event.userId(),
						extractionResult.persons(),
						existingPersonCandidates
				);

		List<MentionCandidate> currentPersonCandidates =
				new ArrayList<>(
						personCandidatesByRef.values()
				);

		reconcilePlaceCandidates(
				event.diaryId(),
				extractionResult.places(),
				personCandidatesByRef,
				existingSourceCandidates
		);

		reconcileActivityCandidates(
				event.diaryId(),
				extractionResult.activities(),
				personCandidatesByRef,
				existingSourceCandidates
		);

		/*
		 * 새 ExtractionResult와 pair되지 못하고 남은
		 * 기존 Candidate가 REMOVE 대상이다.
		 *
		 * Relation은 FK ON DELETE CASCADE로 같이 제거된다.
		 */
		List<MentionCandidate> candidatesToRemove =
				new ArrayList<>();

		candidatesToRemove.addAll(
				collectRemainingCandidates(
						existingPersonCandidates
				)
		);

		candidatesToRemove.addAll(
				collectRemainingCandidates(
						existingSourceCandidates
				)
		);

		mentionCandidateService.deleteAll(
				candidatesToRemove
		);

		/*
		 * Candidate 전체 반영이 끝난 뒤
		 * 현재 확정 PERSON Candidate 기준으로
		 * DiaryPerson을 다시 맞춘다.
		 *
		 * 반환값은 기존 Person + 새 Person의 합집합이므로
		 * Aggregate 재계산 대상에도 그대로 사용할 수 있다.
		 */
		Set<UUID> affectedPersonIds =
				diaryPersonService
						.reconcileDiaryPersons(
								event.diaryId(),
								event.userId(),
								currentPersonCandidates
						);

		personAggregateService.recalculate(
				event.userId(),
				affectedPersonIds
		);
	}

	private Map<String, MentionCandidate>
	reconcilePersonCandidates(
			UUID diaryId,
			UUID userId,
			List<PersonExtraction> persons,
			Map<String, List<MentionCandidate>>
					existingCandidatePool
	) {
		Map<String, MentionCandidate>
				candidatesByRef =
				new LinkedHashMap<>();

		Set<UUID> assignedPersonIds =
				new HashSet<>();

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

			/*
			 * 같은 normalizedText 그룹에서
			 * rawText exact를 먼저 찾고,
			 * 없으면 그룹의 남은 Candidate 하나를
			 * 결정적으로 KEEP한다.
			 */
			MentionCandidate candidate =
					takeExistingCandidate(
							existingCandidatePool,
							normalizedText,
							person.rawText()
					);

			if (candidate != null) {

				/*
				 * KEEP
				 *
				 * Candidate ID / status /
				 * matchedPersonId는 그대로 둔다.
				 * rawText와 normalizedText만
				 * 현재 본문 기준으로 갱신한다.
				 *
				 * Resolver를 다시 실행하지 않는다.
				 */
				candidate.updateText(
						person.rawText(),
						normalizedText
				);

			} else {

				/*
				 * ADD
				 *
				 * 새 PERSON만 Resolver를 실행한다.
				 */
				Set<UUID> reusablePersonIds =
						findReusablePersonIdsInDiary(
								normalizedText,
								assignedPersonIdsByNormalizedText
						);

				if (reusablePersonIds.size() == 1) {

					UUID reusablePersonId =
							reusablePersonIds
									.iterator()
									.next();

					/*
					 * 9.4.1
					 *
					 * 같은 ExtractionResult 안에서
					 * normalizedText가 완전히 같고,
					 * 이미 정확히 한 Person에 배정됐다면
					 * 일반 Matching/Creation Guard를 다시 타지 않고
					 * 그 Person을 직접 재사용한다.
					 */
					candidate =
							mentionCandidateService
									.createPersonCandidateForMatchedPerson(
											diaryId,
											person.rawText(),
											reusablePersonId
									);

				} else {

					Set<UUID> blockedPersonIds =
							new HashSet<>(
									assignedPersonIds
							);

					candidate =
							mentionCandidateService
									.createPersonCandidate(
											diaryId,
											userId,
											person.rawText(),
											blockedPersonIds
									);
				}
			}

			candidatesByRef.put(
					person.ref(),
					candidate
			);

			UUID matchedPersonId =
					candidate.getMatchedPersonId();

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

	private void reconcilePlaceCandidates(
			UUID diaryId,
			List<PlaceExtraction> places,
			Map<String, MentionCandidate>
					personCandidatesByRef,
			Map<String, List<MentionCandidate>>
					existingCandidatePool
	) {
		for (PlaceExtraction place : places) {

			String normalizedText =
					place.normalizedText()
							.strip();

			String key =
					sourceCandidateKey(
							MentionEntityType.PLACE,
							normalizedText
					);

			MentionCandidate sourceCandidate =
					takeExistingCandidate(
							existingCandidatePool,
							key,
							place.rawText()
					);

			if (sourceCandidate != null) {

				/*
				 * KEEP
				 */
				sourceCandidate.updateText(
						place.rawText(),
						normalizedText
				);

			} else {

				/*
				 * ADD
				 */
				sourceCandidate =
						mentionCandidateService
								.createNonPersonCandidate(
										diaryId,
										place.rawText(),
										normalizedText,
										MentionEntityType.PLACE
								);
			}

			List<MentionCandidate> relatedPersons =
					resolvePersonCandidates(
							place.personRefs(),
							personCandidatesByRef
					);

			/*
			 * 신규 source여도 기존 Relation은 빈 목록이므로
			 * reconcileRelations 하나로 ADD/KEEP 모두 처리 가능하다.
			 */
			relationService.reconcileRelations(
					diaryId,
					sourceCandidate,
					relatedPersons
			);
		}
	}

	private void reconcileActivityCandidates(
			UUID diaryId,
			List<ActivityExtraction> activities,
			Map<String, MentionCandidate>
					personCandidatesByRef,
			Map<String, List<MentionCandidate>>
					existingCandidatePool
	) {
		for (ActivityExtraction activity
				: activities) {

			String normalizedText =
					activity.normalizedText()
							.strip();

			String key =
					sourceCandidateKey(
							MentionEntityType.ACTIVITY,
							normalizedText
					);

			MentionCandidate sourceCandidate =
					takeExistingCandidate(
							existingCandidatePool,
							key,
							activity.rawText()
					);

			if (sourceCandidate != null) {

				/*
				 * KEEP
				 */
				sourceCandidate.updateText(
						activity.rawText(),
						normalizedText
				);

			} else {

				/*
				 * ADD
				 */
				sourceCandidate =
						mentionCandidateService
								.createNonPersonCandidate(
										diaryId,
										activity.rawText(),
										normalizedText,
										MentionEntityType.ACTIVITY
								);
			}

			List<MentionCandidate> relatedPersons =
					resolvePersonCandidates(
							activity.personRefs(),
							personCandidatesByRef
					);

			relationService.reconcileRelations(
					diaryId,
					sourceCandidate,
					relatedPersons
			);
		}
	}

	/*
	 * PERSON 기존 Candidate를
	 *
	 * normalizedText
	 * -> List<Candidate>
	 *
	 * 형태로 묶는다.
	 *
	 * 같은 normalizedText가 여러 번 등장하는 Diary도
	 * Candidate 하나로 뭉개지 않는다.
	 */
	private Map<String, List<MentionCandidate>>
	buildPersonCandidatePool(
			List<MentionCandidate> candidates
	) {
		Map<String, List<MentionCandidate>> pool =
				new HashMap<>();

		for (MentionCandidate candidate
				: candidates) {

			if (candidate.getEntityType()
					!= MentionEntityType.PERSON) {
				continue;
			}

			pool.computeIfAbsent(
					candidate.getNormalizedText(),
					key -> new ArrayList<>()
			).add(candidate);
		}

		sortCandidatePool(pool);

		return pool;
	}

	/*
	 * PLACE / ACTIVITY는
	 *
	 * entityType + normalizedText
	 *
	 * 조합이 diff key다.
	 */
	private Map<String, List<MentionCandidate>>
	buildSourceCandidatePool(
			List<MentionCandidate> candidates
	) {
		Map<String, List<MentionCandidate>> pool =
				new HashMap<>();

		for (MentionCandidate candidate
				: candidates) {

			MentionEntityType entityType =
					candidate.getEntityType();

			if (entityType != MentionEntityType.PLACE
					&& entityType
					!= MentionEntityType.ACTIVITY) {
				continue;
			}

			String key =
					sourceCandidateKey(
							entityType,
							candidate.getNormalizedText()
					);

			pool.computeIfAbsent(
					key,
					ignored -> new ArrayList<>()
			).add(candidate);
		}

		sortCandidatePool(pool);

		return pool;
	}

	/*
	 * DB에는 Extraction occurrence 순서 컬럼이 없으므로
	 * 기존 Candidate 그룹 내부 순서는
	 * rawText -> UUID 순서로 고정한다.
	 *
	 * 실제 pair에서는 rawText exact가 항상 우선한다.
	 */
	private void sortCandidatePool(
			Map<String, List<MentionCandidate>> pool
	) {
		Comparator<MentionCandidate> comparator =
				Comparator
						.comparing(
								MentionCandidate::getRawText
						)
						.thenComparing(
								MentionCandidate::getId
						);

		for (List<MentionCandidate> candidates
				: pool.values()) {

			candidates.sort(comparator);
		}
	}

	/*
	 * KEEP Candidate 선택 규칙:
	 *
	 * 1. 같은 diff key
	 * 2. 그 안에서 rawText exact 우선
	 * 3. exact가 없으면 남은 Candidate 첫 번째
	 *
	 * 선택한 Candidate는 pool에서 제거하므로
	 * 같은 Candidate를 두 occurrence가 동시에 KEEP하지 않는다.
	 */
	private MentionCandidate takeExistingCandidate(
			Map<String, List<MentionCandidate>> pool,
			String key,
			String rawText
	) {
		List<MentionCandidate> candidates =
				pool.get(key);

		if (candidates == null
				|| candidates.isEmpty()) {
			return null;
		}

		int selectedIndex = -1;

		for (int i = 0;
			 i < candidates.size();
			 i++) {

			if (rawText.equals(
					candidates.get(i)
							.getRawText()
			)) {
				selectedIndex = i;
				break;
			}
		}

		if (selectedIndex < 0) {
			selectedIndex = 0;
		}

		MentionCandidate selected =
				candidates.remove(
						selectedIndex
				);

		if (candidates.isEmpty()) {
			pool.remove(key);
		}

		return selected;
	}

	private List<MentionCandidate>
	collectRemainingCandidates(
			Map<String, List<MentionCandidate>> pool
	) {
		List<MentionCandidate> remaining =
				new ArrayList<>();

		for (List<MentionCandidate> candidates
				: pool.values()) {

			remaining.addAll(candidates);
		}

		return remaining;
	}

	private String sourceCandidateKey(
			MentionEntityType entityType,
			String normalizedText
	) {
		return entityType.name()
				+ "\u0000"
				+ normalizedText;
	}

	private Set<UUID> findReusablePersonIdsInDiary(
			String normalizedText,
			Map<String, Set<UUID>>
					assignedPersonIdsByNormalizedText
	) {
		return new HashSet<>(
				assignedPersonIdsByNormalizedText
						.getOrDefault(
								normalizedText,
								Set.of()
						)
		);
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