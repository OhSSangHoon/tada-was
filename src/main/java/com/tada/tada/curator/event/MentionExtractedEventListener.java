package com.tada.tada.curator.event;

import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.service.DiaryPersonService;
import com.tada.tada.curator.service.MentionCandidatePersonRefService;
import com.tada.tada.curator.service.MentionCandidateService;
import com.tada.tada.curator.service.PersonAggregateService;
import com.tada.tada.curator.validation.ExtractionResultValidator;
import com.tada.tada.diary.entity.Diary;
import com.tada.tada.diary.repository.DiaryRepository;
import com.tada.tada.global.event.MentionExtractedEvent;
import com.tada.tada.global.event.dto.ActivityExtraction;
import com.tada.tada.global.event.dto.ExtractionResult;
import com.tada.tada.global.event.dto.PersonExtraction;
import com.tada.tada.global.event.dto.PlaceExtraction;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MentionExtractedEventListener {

	private final DiaryRepository diaryRepository;
	private final ExtractionResultValidator extractionResultValidator;
	private final MentionCandidateService mentionCandidateService;
	private final MentionCandidatePersonRefService relationService;
	private final DiaryPersonService diaryPersonService;
	private final PersonAggregateService personAggregateService;

	@EventListener
	@Transactional
	public void handle(
			MentionExtractedEvent event
	) {
		validateEvent(event);

		Diary diary =
				findAndValidateDiary(
						event.diaryId(),
						event.userId()
				);

		if (mentionCandidateService.hasCandidates(
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

		Map<String, MentionCandidate> personCandidatesByRef =
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
				diaryPersonService.reconcileDiaryPersons(
						event.diaryId(),
						event.userId(),
						personCandidates
				);

		personAggregateService.recalculate(
				event.userId(),
				affectedPersonIds
		);
	}

	private Map<String, MentionCandidate> createPersonCandidates(
			UUID diaryId,
			UUID userId,
			List<PersonExtraction> persons
	) {
		Map<String, MentionCandidate> candidatesByRef =
				new HashMap<>();

		Set<UUID> assignedPersonIds =
				new HashSet<>();

		for (PersonExtraction person : persons) {
			MentionCandidate candidate =
					mentionCandidateService
							.createPersonCandidate(
									diaryId,
									userId,
									person.rawText(),
									assignedPersonIds
							);

			candidatesByRef.put(
					person.ref(),
					candidate
			);

			assignedPersonIds.add(
					candidate.getMatchedPersonId()
			);
		}

		return candidatesByRef;
	}

	private void createPlaceCandidates(
			UUID diaryId,
			List<PlaceExtraction> places,
			Map<String, MentionCandidate> personCandidatesByRef
	) {
		for (PlaceExtraction place : places) {
			MentionCandidate sourceCandidate =
					mentionCandidateService
							.createNonPersonCandidate(
									diaryId,
									place.rawText(),
									place.normalizedText(),
									"PLACE"
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
			Map<String, MentionCandidate> personCandidatesByRef
	) {
		for (ActivityExtraction activity : activities) {
			MentionCandidate sourceCandidate =
					mentionCandidateService
							.createNonPersonCandidate(
									diaryId,
									activity.rawText(),
									activity.normalizedText(),
									"ACTIVITY"
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

	private List<MentionCandidate> resolvePersonCandidates(
			List<String> personRefs,
			Map<String, MentionCandidate> personCandidatesByRef
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

			persons.add(
					candidate
			);
		}

		return persons;
	}

	private Diary findAndValidateDiary(
			UUID diaryId,
			UUID userId
	) {
		Diary diary =
				diaryRepository.findById(
						diaryId
				).orElseThrow(
						() -> new IllegalStateException(
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