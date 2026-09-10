package com.tada.tada.curator.dto;

import lombok.Getter;

/*
 * 한눈에 보기 칩 데이터. 화면 문장(예: "광안리 5회")은 프론트가 조합한다.
 */
@Getter
public class PersonEntityStatResponse {

	private final String normalizedText;
	private final long diaryCount;

	public PersonEntityStatResponse(
			String normalizedText,
			long diaryCount
	) {
		this.normalizedText = normalizedText;
		this.diaryCount = diaryCount;
	}
}
