package com.tada.tada.curator.model;

import java.util.List;

public record PersonNormalization(
		String normalizedText,
		List<String> strongMatchCandidates,
		List<String> weakMatchCandidates
) {
	public PersonNormalization {
		normalizedText = normalizedText == null
				? ""
				: normalizedText;

		strongMatchCandidates = strongMatchCandidates == null
				? List.of()
				: List.copyOf(strongMatchCandidates);

		weakMatchCandidates = weakMatchCandidates == null
				? List.of()
				: List.copyOf(weakMatchCandidates);
	}
}