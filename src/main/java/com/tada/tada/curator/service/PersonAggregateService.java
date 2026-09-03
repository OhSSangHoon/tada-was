package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.MemoryPerson;
import com.tada.tada.curator.entity.PersonAggregate;
import com.tada.tada.curator.repository.MemoryPersonRepository;
import com.tada.tada.curator.repository.PersonAggregateRepository;
import com.tada.tada.curator.repository.PersonAggregateRepository.PersonAggregateSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonAggregateService {

	private final PersonAggregateRepository personAggregateRepository;
	private final MemoryPersonRepository memoryPersonRepository;

	@Transactional
	public void recalculate(
			UUID userId,
			Collection<UUID> affectedPersonIds
	) {
		if (userId == null) {
			throw new IllegalArgumentException(
					"userId must not be null"
			);
		}

		if (affectedPersonIds == null) {
			throw new IllegalArgumentException(
					"affectedPersonIds must not be null"
			);
		}

		Set<UUID> personIds =
				new HashSet<>(
						affectedPersonIds
				);

		if (personIds.isEmpty()) {
			return;
		}

		validatePersonOwners(
				userId,
				personIds
		);

		Map<UUID, PersonAggregateSnapshot> snapshots =
				loadSnapshots(
						userId,
						personIds
				);

		Map<UUID, PersonAggregate> existingAggregates =
				loadExistingAggregates(
						personIds
				);

		List<PersonAggregate> aggregatesToSave =
				new ArrayList<>();

		for (UUID personId : personIds) {

			PersonAggregateSnapshot snapshot =
					snapshots.get(
							personId
					);

			int mentionCount =
					snapshot == null
							? 0
							: Math.toIntExact(
							snapshot.getMentionCount()
					);

			LocalDate lastMentionedDate =
					snapshot == null
							? null
							: snapshot.getLastMentionedDate();

			PersonAggregate aggregate =
					existingAggregates.get(
							personId
					);

			if (aggregate == null) {
				aggregate =
						PersonAggregate.create(
								personId,
								mentionCount,
								lastMentionedDate
						);
			} else {
				aggregate.applySnapshot(
						mentionCount,
						lastMentionedDate
				);
			}

			aggregatesToSave.add(
					aggregate
			);
		}

		personAggregateRepository.saveAll(
				aggregatesToSave
		);
	}

	private Map<UUID, PersonAggregateSnapshot>
	loadSnapshots(
			UUID userId,
			Set<UUID> personIds
	) {
		List<PersonAggregateSnapshot> snapshots =
				personAggregateRepository
						.findActiveSnapshots(
								userId,
								personIds
						);

		Map<UUID, PersonAggregateSnapshot> snapshotMap =
				new HashMap<>();

		for (PersonAggregateSnapshot snapshot
				: snapshots) {

			snapshotMap.put(
					snapshot.getPersonId(),
					snapshot
			);
		}

		return snapshotMap;
	}

	private Map<UUID, PersonAggregate>
	loadExistingAggregates(
			Set<UUID> personIds
	) {
		List<PersonAggregate> aggregates =
				personAggregateRepository
						.findAllById(
								personIds
						);

		Map<UUID, PersonAggregate> aggregateMap =
				new HashMap<>();

		for (PersonAggregate aggregate
				: aggregates) {

			aggregateMap.put(
					aggregate.getPersonId(),
					aggregate
			);
		}

		return aggregateMap;
	}

	private void validatePersonOwners(
			UUID userId,
			Set<UUID> personIds
	) {
		List<MemoryPerson> persons =
				memoryPersonRepository
						.findAllById(
								personIds
						);

		if (persons.size()
				!= personIds.size()) {
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