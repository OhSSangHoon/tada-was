package com.tada.tada.curator.model;

import java.util.List;

public record PersonNormalization(
		String normalizedText,
		String displayNameCandidate,
		List<String> strongMatchCandidates,
		List<String> safeMatchCandidates,
		List<String> weakMatchCandidates
) {
	public PersonNormalization {
		normalizedText = normalizedText == null
				? ""
				: normalizedText;

		/*
		 * 신규 MemoryPerson 표시 이름 후보. normalizedText와 달리
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

		/*
		 * strongMatchCandidates 중 안전 조사 제거만으로 도달한 부분집합
		 * (애매한 조사 은/이/도/랑/님/씨/아 제거형은 제외).
		 * PersonMatchingService의 EXACT 판정은 이것만 써서 "김성은"과 "김성" 같은
		 * 서로 다를 수 있는 이름이 자동으로 합쳐지지 않게 한다.
		 */
		safeMatchCandidates = safeMatchCandidates == null
				? List.of()
				: List.copyOf(safeMatchCandidates);

		weakMatchCandidates = weakMatchCandidates == null
				? List.of()
				: List.copyOf(weakMatchCandidates);
	}
}
