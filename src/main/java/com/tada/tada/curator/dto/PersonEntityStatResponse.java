package com.tada.tada.curator.dto;

/*
 * 사람 상세 `한눈에 보기` 의 칩 하나.
 * 화면에는 `광안리 5회` 로 그린다. 문장은 프론트가 만든다.
 */
public record PersonEntityStatResponse(
		String normalizedText,
		long diaryCount
) {
}
