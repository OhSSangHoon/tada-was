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
						: Set.copyOf(
						blockedPersonIds
				);

		PersonNormalization normalization =
				personNormalizer.normalize(
						rawText
				);

		if (normalization
				.normalizedText()
				.isBlank()) {

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
			case EXACT, SIMILAR ->
					requireMatchedPerson(
							userId,
							matchResult,
							blockedIds
					);

			case AMBIGUOUS, NEW ->
					resolveAmbiguousOrNew(
							userId,
							rawText,
							normalization,
							blockedIds,
							matchResult
					);
		};
	}

	private UUID requireMatchedPerson(
			UUID userId,
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

		if (blockedPersonIds.contains(
				personId
		)) {
			throw new IllegalStateException(
					"blocked person cannot be automatically matched"
			);
		}

		return validatePersonOwner(
				userId,
				personId
		);
	}

	private UUID resolveAmbiguousOrNew(
			UUID userId,
			String rawText,
			PersonNormalization normalization,
			Set<UUID> blockedPersonIds,
			PersonMatchResult matchResult
	) {
		Optional<UUID> reusablePersonId =
				personCreationGuard
						.findReusablePerson(
								userId,
								rawText,
								normalization
										.normalizedText(),
								blockedPersonIds
						);

		if (reusablePersonId.isPresent()
				&& canReuseMatchCandidate(
				matchResult,
				reusablePersonId.get()
		)) {

			UUID personId =
					reusablePersonId.get();

			if (blockedPersonIds.contains(
					personId
			)) {
				throw new IllegalStateException(
						"blocked person cannot be reused"
				);
			}

			return validatePersonOwner(
					userId,
					personId
			);
		}

		/*
		 * 표시 이름에는 매칭용 normalizedText 가 아니라
		 * displayNameCandidate 를 사용한다.
		 *
		 * "은", "이" 는 조사일 수도 실제 이름의 끝 글자일 수도 있어
		 * 매칭 후보로만 제거하고 표시 이름에는 반영하지 않는다.
		 *
		 *   "김성은" -> "김성" 으로 줄이지 않는다
		 *   "가을이" -> "가을" 로 줄이지 않는다
		 *
		 * 같은 사람이 나중에 다른 조사로 등장하면
		 * PersonMatchingService 의 정규화 exact 단계가 다시 연결한다.
		 */
		return createPerson(
				userId,
				normalization.displayNameCandidate()
		);
	}

	private boolean canReuseMatchCandidate(
			PersonMatchResult matchResult,
			UUID reusablePersonId
	) {
		return matchResult
				.candidatePersonIds()
				.isEmpty()

				|| matchResult
				.candidatePersonIds()
				.contains(
						reusablePersonId
				);
	}

	private UUID validatePersonOwner(
			UUID userId,
			UUID personId
	) {
		MemoryPerson person =
				memoryPersonRepository
						.findById(personId)
						.orElseThrow(
								() ->
										new IllegalStateException(
												"matched person does not exist"
										)
						);

		if (!userId.equals(
				person.getUserId()
		)) {
			throw new IllegalStateException(
					"matched person belongs to another user"
			);
		}

		return personId;
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
				memoryPersonRepository.save(
						person
				);

		return savedPerson.getId();
	}
}