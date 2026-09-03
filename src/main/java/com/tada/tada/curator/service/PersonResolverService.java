package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.MemoryPerson;
import com.tada.tada.curator.model.PersonMatchResult;
import com.tada.tada.curator.model.PersonNormalization;
import com.tada.tada.curator.repository.MemoryPersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonResolverService {

	private final PersonMatchingService personMatchingService;
	private final PersonCreationGuard personCreationGuard;
	private final PersonNormalizer personNormalizer;
	private final MemoryPersonRepository memoryPersonRepository;

	public UUID resolve(
			UUID userId,
			String rawText
	) {
		return resolve(
				userId,
				rawText,
				Set.of()
		);
	}

	public UUID resolve(
			UUID userId,
			String rawText,
			Set<UUID> blockedPersonIds
	) {
		if (userId == null) {
			throw new IllegalArgumentException(
					"userId must not be null"
			);
		}

		Set<UUID> blockedIds =
				blockedPersonIds == null
						? Set.of()
						: Set.copyOf(blockedPersonIds);

		PersonNormalization normalization =
				personNormalizer.normalize(rawText);

		if (normalization.normalizedText().isBlank()) {
			throw new IllegalArgumentException(
					"person name must not be blank"
			);
		}

		PersonMatchResult matchResult =
				personMatchingService.match(
						userId,
						rawText,
						blockedIds
				);

		return switch (matchResult.matchType()) {
			case EXACT, SIMILAR -> requireMatchedPerson(
					matchResult,
					blockedIds
			);

			case AMBIGUOUS, NEW -> resolveAmbiguousOrNew(
					userId,
					rawText,
					normalization.normalizedText(),
					blockedIds
			);
		};
	}

	private UUID requireMatchedPerson(
			PersonMatchResult matchResult,
			Set<UUID> blockedPersonIds
	) {
		UUID personId =
				matchResult.matchedPersonId();

		if (personId == null) {
			throw new IllegalStateException(
					"matched personId is required for "
							+ matchResult.matchType()
			);
		}

		if (blockedPersonIds.contains(personId)) {
			throw new IllegalStateException(
					"blocked person cannot be automatically matched"
			);
		}

		return personId;
	}

	private UUID resolveAmbiguousOrNew(
			UUID userId,
			String rawText,
			String normalizedText,
			Set<UUID> blockedPersonIds
	) {
		Optional<UUID> reusablePersonId =
				personCreationGuard.findReusablePerson(
						userId,
						rawText,
						normalizedText,
						blockedPersonIds
				);

		if (reusablePersonId.isPresent()) {
			return validateReusablePerson(
					userId,
					reusablePersonId.get()
			);
		}

		return createPerson(
				userId,
				normalizedText
		);
	}

	private UUID validateReusablePerson(
			UUID userId,
			UUID personId
	) {
		MemoryPerson person =
				memoryPersonRepository.findById(personId)
						.orElseThrow(
								() -> new IllegalStateException(
										"reusable person does not exist"
								)
						);

		if (!userId.equals(person.getUserId())) {
			throw new IllegalStateException(
					"reusable person belongs to another user"
			);
		}

		return person.getId();
	}

	private UUID createPerson(
			UUID userId,
			String displayName
	) {
		MemoryPerson person =
				MemoryPerson.create(
						userId,
						displayName
				);

		MemoryPerson savedPerson =
				memoryPersonRepository.save(person);

		return savedPerson.getId();
	}
}