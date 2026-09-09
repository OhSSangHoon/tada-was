package com.tada.tada.curator.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/*
 * 헤더+통계+한눈에 보기를 1회 응답으로 묶는다 (타임라인·추억 그룹은 별도 API).
 * displayName은 조사 없는 원형(문장 조합은 프론트), 날짜는 ACTIVE 일기 없으면 null.
 */
public record PersonDetailResponse(
		UUID id,
		String displayName,
		String stickerUrl,
		int mentionCount,
		LocalDate firstMentionedAt,
		LocalDate lastMentionedAt,
		List<PersonEntityStatResponse> topPlaces,
		List<PersonEntityStatResponse> topActivities
) {
}
