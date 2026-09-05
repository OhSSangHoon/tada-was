package com.tada.tada.curator.model;

import java.util.List;

public record PersonNormalization(
		String normalizedText,
		String displayNameCandidate,
		List<String> strongMatchCandidates,
		List<String> weakMatchCandidates
) {
	public PersonNormalization {
		normalizedText = normalizedText == null
				? ""
				: normalizedText;

		/*
		 * 신규 MemoryPerson 의 표시 이름 후보다.
		 *
		 * 매칭용 normalizedText 와 달리
		 * 이름 끝 글자일 수 있는 애매한 접미사를 보존한다.
		 */
		displayNameCandidate =
				displayNameCandidate == null
						|| displayNameCandidate.isBlank()
						? normalizedText
						: displayNameCandidate;

		strongMatchCandidates = strongMatchCandidates == null
				? List.of()
				: List.copyOf(strongMatchCandidates);

		weakMatchCandidates = weakMatchCandidates == null
				? List.of()
				: List.copyOf(weakMatchCandidates);
	}
}
