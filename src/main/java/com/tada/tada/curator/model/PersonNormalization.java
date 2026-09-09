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

		/*
		 * strongMatchCandidates 중 안전 조사 제거만으로 도달한 형태.
		 * 애매한 조사(은/이/도/랑/님/씨/아)를 떼야만 나오는 형태는 뺀다.
		 *
		 * PersonMatchingService 의 EXACT 판정(findExactResult /
		 * findNormalizedExactResult)은 이 후보만 쓴다. "김성은" 과
		 * "김성" 처럼 실제로 다른 사람일 수 있는 형태를 EXACT 로
		 * 자동 연결하지 않기 위해서다.
		 */
		safeMatchCandidates = safeMatchCandidates == null
				? List.of()
				: List.copyOf(safeMatchCandidates);

		weakMatchCandidates = weakMatchCandidates == null
				? List.of()
				: List.copyOf(weakMatchCandidates);
	}
}
