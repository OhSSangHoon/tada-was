package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.DiaryPerson;
import com.tada.tada.curator.entity.MemoryPerson;
import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.entity.MentionCandidateStatus;
import com.tada.tada.curator.repository.DiaryPersonRepository;
import com.tada.tada.curator.repository.MemoryPersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiaryPersonService {

	private final DiaryPersonRepository diaryPersonRepository;
	private final MemoryPersonRepository memoryPersonRepository;

	public Set<UUID> reconcileDiaryPersons(
			UUID diaryId,
			UUID userId,
			Collection<MentionCandidate> personCandidates
	) {
		validateRequest(
				diaryId,
				userId,
				personCandidates
		);

		Set<UUID> desiredPersonIds =
				collectDesiredPersonIds(
						diaryId,
						personCandidates
				);

		validatePersonOwners(
				userId,
				desiredPersonIds
		);

		List<DiaryPerson> existingDiaryPersons =
				diaryPersonRepository.findAllByDiaryId(
						diaryId
				);

		Set<UUID> existingPersonIds =
				new HashSet<>();

		for (DiaryPerson diaryPerson : existingDiaryPersons) {
			existingPersonIds.add(
					diaryPerson.getPersonId()
			);
		}

		List<DiaryPerson> relationsToRemove =
				new ArrayList<>();

		for (DiaryPerson diaryPerson : existingDiaryPersons) {
			if (!desiredPersonIds.contains(
					diaryPerson.getPersonId()
			)) {
				relationsToRemove.add(
						diaryPerson
				);
			}
		}

		List<DiaryPerson> relationsToAdd =
				new ArrayList<>();

		for (UUID personId : desiredPersonIds) {
			if (existingPersonIds.contains(
					personId
			)) {
				continue;
			}

			relationsToAdd.add(
					DiaryPerson.create(
							diaryId,
							personId
					)
			);
		}

		if (!relationsToRemove.isEmpty()) {
			diaryPersonRepository.deleteAll(
					relationsToRemove
			);
		}

		if (!relationsToAdd.isEmpty()) {
			diaryPersonRepository.saveAll(
					relationsToAdd
			);
		}

		Set<UUID> affectedPersonIds =
				new HashSet<>(
						existingPersonIds
				);

		affectedPersonIds.addAll(
				desiredPersonIds
		);

		return Set.copyOf(
				affectedPersonIds
		);
	}

	private void validateRequest(
			UUID diaryId,
			UUID userId,
			Collection<MentionCandidate> personCandidates
	) {
		if (diaryId == null) {
			throw new IllegalArgumentException(
					"diaryId must not be null"
			);
		}

		if (userId == null) {
			throw new IllegalArgumentException(
					"userId must not be null"
			);
		}

		if (personCandidates == null) {
			throw new IllegalArgumentException(
					"personCandidates must not be null"
			);
		}
	}

	private Set<UUID> collectDesiredPersonIds(
			UUID diaryId,
			Collection<MentionCandidate> personCandidates
	) {
		Set<UUID> personIds =
				new HashSet<>();

		for (MentionCandidate candidate : personCandidates) {
			validatePersonCandidate(
					diaryId,
					candidate
			);

			personIds.add(
					candidate.getMatchedPersonId()
			);
		}

		return personIds;
	}

	private void validatePersonCandidate(
			UUID diaryId,
			MentionCandidate candidate
	) {
		if (candidate == null) {
			throw new IllegalArgumentException(
					"personCandidate must not be null"
			);
		}

		if (!diaryId.equals(
				candidate.getDiaryId()
		)) {
			throw new IllegalStateException(
					"personCandidate belongs to another diary"
			);
		}

		if (!"PERSON".equals(
				candidate.getEntityType()
		)) {
			throw new IllegalStateException(
					"candidate must be PERSON"
			);
		}

		if (candidate.getStatus()
				!= MentionCandidateStatus.CONFIRMED) {
			throw new IllegalStateException(
					"PERSON candidate must be CONFIRMED"
			);
		}

		if (candidate.getMatchedPersonId() == null) {
			throw new IllegalStateException(
					"PERSON candidate matchedPersonId must not be null"
			);
		}
	}

	private void validatePersonOwners(
			UUID userId,
			Set<UUID> personIds
	) {
		if (personIds.isEmpty()) {
			return;
		}

		List<MemoryPerson> persons =
				memoryPersonRepository.findAllById(
						personIds
				);

		if (persons.size() != personIds.size()) {
			throw new IllegalStateException(
					"one or more persons do not exist"
			);
		}

		for (MemoryPerson person : persons) {
			if (!userId.equals(
					person.getUserId()
			)) {
				throw new IllegalStateException(
						"person belongs to another user"
				);
			}
		}
	}
}