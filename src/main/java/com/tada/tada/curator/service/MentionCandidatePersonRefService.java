package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.entity.MentionCandidatePersonRef;
import com.tada.tada.curator.entity.MentionCandidateStatus;
import com.tada.tada.curator.entity.MentionEntityType;
import com.tada.tada.curator.repository.MentionCandidatePersonRefRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MentionCandidatePersonRefService {

	private final MentionCandidatePersonRefRepository relationRepository;

	public void createRelations(
			UUID diaryId,
			MentionCandidate sourceCandidate,
			Collection<MentionCandidate> personCandidates
	) {
		validateRequest(
				diaryId,
				sourceCandidate,
				personCandidates
		);

		validateSourceCandidate(
				diaryId,
				sourceCandidate
		);

		Map<UUID, MentionCandidate> desiredPersons =
				validateAndCollectPersonCandidates(
						diaryId,
						sourceCandidate,
						personCandidates
				);

		if (desiredPersons.isEmpty()) {
			return;
		}

		List<MentionCandidatePersonRef> relations =
				new ArrayList<>();

		for (UUID personCandidateId
				: desiredPersons.keySet()) {

			relations.add(
					MentionCandidatePersonRef.create(
							sourceCandidate.getId(),
							personCandidateId
					)
			);
		}

		relationRepository.saveAll(
				relations
		);
	}

	public void reconcileRelations(
			UUID diaryId,
			MentionCandidate sourceCandidate,
			Collection<MentionCandidate> personCandidates
	) {
		validateRequest(
				diaryId,
				sourceCandidate,
				personCandidates
		);

		validateSourceCandidate(
				diaryId,
				sourceCandidate
		);

		Map<UUID, MentionCandidate> desiredPersons =
				validateAndCollectPersonCandidates(
						diaryId,
						sourceCandidate,
						personCandidates
				);

		List<MentionCandidatePersonRef> existingRelations =
				relationRepository.findAllBySourceCandidateId(
						sourceCandidate.getId()
				);

		Set<UUID> existingPersonCandidateIds =
				new HashSet<>();

		for (MentionCandidatePersonRef relation
				: existingRelations) {

			existingPersonCandidateIds.add(
					relation.getPersonCandidateId()
			);
		}

		List<MentionCandidatePersonRef> relationsToRemove =
				new ArrayList<>();

		for (MentionCandidatePersonRef relation
				: existingRelations) {

			if (!desiredPersons.containsKey(
					relation.getPersonCandidateId()
			)) {
				relationsToRemove.add(
						relation
				);
			}
		}

		List<MentionCandidatePersonRef> relationsToAdd =
				new ArrayList<>();

		for (UUID personCandidateId
				: desiredPersons.keySet()) {

			if (existingPersonCandidateIds.contains(
					personCandidateId
			)) {
				continue;
			}

			relationsToAdd.add(
					MentionCandidatePersonRef.create(
							sourceCandidate.getId(),
							personCandidateId
					)
			);
		}

		if (!relationsToRemove.isEmpty()) {
			relationRepository.deleteAll(
					relationsToRemove
			);
		}

		if (!relationsToAdd.isEmpty()) {
			relationRepository.saveAll(
					relationsToAdd
			);
		}
	}

	private void validateRequest(
			UUID diaryId,
			MentionCandidate sourceCandidate,
			Collection<MentionCandidate> personCandidates
	) {
		if (diaryId == null) {
			throw new IllegalArgumentException(
					"diaryId must not be null"
			);
		}

		if (sourceCandidate == null) {
			throw new IllegalArgumentException(
					"sourceCandidate must not be null"
			);
		}

		if (personCandidates == null) {
			throw new IllegalArgumentException(
					"personCandidates must not be null"
			);
		}
	}

	private void validateSourceCandidate(
			UUID diaryId,
			MentionCandidate sourceCandidate
	) {
		if (sourceCandidate.getId() == null) {
			throw new IllegalStateException(
					"sourceCandidate id must not be null"
			);
		}

		if (!diaryId.equals(
				sourceCandidate.getDiaryId()
		)) {
			throw new IllegalStateException(
					"sourceCandidate belongs to another diary"
			);
		}

		if (sourceCandidate.getEntityType() == null
				|| !sourceCandidate.getEntityType().isSource()) {
			throw new IllegalStateException(
					"sourceCandidate must be PLACE or ACTIVITY"
			);
		}

		if (sourceCandidate.getStatus()
				!= MentionCandidateStatus.CONFIRMED) {
			throw new IllegalStateException(
					"sourceCandidate must be CONFIRMED"
			);
		}

		if (sourceCandidate.getMatchedPersonId()
				!= null) {
			throw new IllegalStateException(
					"sourceCandidate matchedPersonId must be null"
			);
		}
	}

	private Map<UUID, MentionCandidate>
	validateAndCollectPersonCandidates(
			UUID diaryId,
			MentionCandidate sourceCandidate,
			Collection<MentionCandidate> personCandidates
	) {
		Map<UUID, MentionCandidate> persons =
				new LinkedHashMap<>();

		for (MentionCandidate personCandidate
				: personCandidates) {

			validatePersonCandidate(
					diaryId,
					sourceCandidate,
					personCandidate
			);

			persons.put(
					personCandidate.getId(),
					personCandidate
			);
		}

		return persons;
	}

	private void validatePersonCandidate(
			UUID diaryId,
			MentionCandidate sourceCandidate,
			MentionCandidate personCandidate
	) {
		if (personCandidate == null) {
			throw new IllegalArgumentException(
					"personCandidate must not be null"
			);
		}

		if (personCandidate.getId() == null) {
			throw new IllegalStateException(
					"personCandidate id must not be null"
			);
		}

		if (sourceCandidate.getId().equals(
				personCandidate.getId()
		)) {
			throw new IllegalStateException(
					"sourceCandidate and personCandidate must be different"
			);
		}

		if (!diaryId.equals(
				personCandidate.getDiaryId()
		)) {
			throw new IllegalStateException(
					"personCandidate belongs to another diary"
			);
		}

		if (personCandidate.getEntityType()
				!= MentionEntityType.PERSON) {
			throw new IllegalStateException(
					"personCandidate must be PERSON"
			);
		}

		if (personCandidate.getStatus()
				!= MentionCandidateStatus.CONFIRMED) {
			throw new IllegalStateException(
					"personCandidate must be CONFIRMED"
			);
		}

		if (personCandidate.getMatchedPersonId()
				== null) {
			throw new IllegalStateException(
					"PERSON candidate matchedPersonId must not be null"
			);
		}
	}
}