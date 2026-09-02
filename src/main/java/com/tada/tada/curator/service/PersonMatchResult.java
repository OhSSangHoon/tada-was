package com.tada.tada.curator.service;

import java.util.List;
import java.util.UUID;

public record PersonMatchResult (
		PersonMatchType matchType,
		UUID matchedPersonId,
		List<UUID> candidatePersonIds
) {
	public PersonMatchResult {
		candidatePersonIds = candidatePersonIds == null
				? List.of()
				: List.copyOf(candidatePersonIds);
	}
	
	public static PersonMatchResult exact(UUID personId) {
		return new PersonMatchResult(
				PersonMatchType.EXACT,
				personId,
				List.of());
	}
	
	public static PersonMatchResult similar(List<UUID> candidatePersonIds) {
		return new PersonMatchResult(
				PersonMatchType.SIMILAR,
				null,
				candidatePersonIds);
	}
	
	public static PersonMatchResult ambiguous(List<UUID> candidatePersonIds) {
		return new PersonMatchResult(
				PersonMatchType.AMBIGUOUS,
				null,
				candidatePersonIds);
	}
	
	public static PersonMatchResult newPerson() {
		return new PersonMatchResult(
				PersonMatchType.NEW,
				null,
				List.of());
	}
}



