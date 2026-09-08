package com.tada.tada.curator.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record PersonMatchResult(
		PersonMatchType matchType,
		UUID matchedPersonId,
		List<UUID> candidatePersonIds
) {
	public PersonMatchResult {
		Objects.requireNonNull(
				matchType,
				"matchType must not be null"
		);

		candidatePersonIds = candidatePersonIds == null
				? List.of()
				: List.copyOf(candidatePersonIds);
	}

	public static PersonMatchResult exact(UUID personId) {
		Objects.requireNonNull(
				personId,
				"personId must not be null"
		);

		return new PersonMatchResult(
				PersonMatchType.EXACT,
				personId,
				List.of()
		);
	}

	public static PersonMatchResult similar(UUID personId) {
		Objects.requireNonNull(
				personId,
				"personId must not be null"
		);

		return new PersonMatchResult(
				PersonMatchType.SIMILAR,
				personId,
				List.of()
		);
	}

	public static PersonMatchResult ambiguous(
			List<UUID> candidatePersonIds
	) {
		return new PersonMatchResult(
				PersonMatchType.AMBIGUOUS,
				null,
				candidatePersonIds
		);
	}

	public static PersonMatchResult newPerson() {
		return new PersonMatchResult(
				PersonMatchType.NEW,
				null,
				List.of()
		);
	}
}