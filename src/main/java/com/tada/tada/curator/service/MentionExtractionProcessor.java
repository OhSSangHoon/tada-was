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
 * Curator 후처리 본체. 일기 저장 트랜잭션에 동기 참여한다 (REQUIRES_NEW 금지 — 별도 트랜잭션이면 Diary만 commit된다).
 * Row lock·기존 Candidate 확인은 신규 저장 경로에서는 안 걸리지만 본문 수정 재추출(명세 12)에 필요해 남겨 둔다.
 * 범위: "최초 이벤트 중복 수신"만 막는다 — 재추출까지의 완전한 멱등성은 별도 reconcile 구조가 필요하다 (명세 12).
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
	 * MANDATORY다 — 상위 트랜잭션 없으면 즉시 예외. REQUIRED면 트랜잭션 밖 발행 시
	 * Curator 데이터만 별도 commit되어 Diary 없이 인물 데이터가 남을 수 있다 (명세 17.1).
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
		 * Diary row lock 이후에 확인한다 — lock 전에 검사하면 동시 진입한 두 이벤트가 모두 통과할 수 있다.
		 * 최초 처리 중복만 막고, 본문 수정 재추출은 별도 reconcile이 필요하다.
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
		 * AI 배열 순서를 유지한다 (명세 10.1-7) — HashMap이면 values() 순서가 비결정적이다.
		 */
		Map<String, MentionCandidate>
				candidatesByRef =
				new LinkedHashMap<>();
		
		Set<UUID> assignedPersonIds =
				new HashSet<>();
		
		/*
		 * 같은 ExtractionResult 안에서는 normalizedText가 완전히 같은 ref만
		 * 동일 인물 재사용 예외를 허용한다.
		 * 정확히 1명의 Person에만 배정된 경우에만 BLOCK을 해제한다.
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
	 * 이번 ExtractionResult 안에서 normalizedText가 완전히 같은 ref에
	 * 이미 배정된 personId만 재사용 후보로 본다.
	 * 후보가 정확히 1명일 때만 호출부가 BLOCK을 해제한다.
	 */
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