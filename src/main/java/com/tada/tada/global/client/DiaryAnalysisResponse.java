package com.tada.tada.global.client;

import com.tada.tada.global.event.dto.ActivityExtraction;
import com.tada.tada.global.event.dto.ExtractionResult;
import com.tada.tada.global.event.dto.PersonExtraction;
import com.tada.tada.global.event.dto.PlaceExtraction;

import java.util.List;

/*
 * n8n diary-analysis 웹훅의 원본 응답 형태.
 * title/compressedKeyword/extractedKeywords는 일기 작성(제목·키워드 생성) 흐름 전용이라
 * 일기 수정(재추출) 흐름에서는 persons/places/activities만 꺼내 쓴다.
 */
public record DiaryAnalysisResponse(
		String title,
		String compressedKeyword,
		List<String> extractedKeywords,
		List<PersonExtraction> persons,
		List<PlaceExtraction> places,
		List<ActivityExtraction> activities
) {
	public ExtractionResult toExtractionResult() {
		return new ExtractionResult(persons, places, activities);
	}
}
