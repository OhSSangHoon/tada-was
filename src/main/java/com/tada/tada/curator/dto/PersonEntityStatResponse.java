package com.tada.tada.curator.dto;

/*
 * 한눈에 보기 칩 데이터. 화면 문장(예: "광안리 5회")은 프론트가 조합한다.
 */
public record PersonEntityStatResponse(
		String normalizedText,
		long diaryCount
) {
}
