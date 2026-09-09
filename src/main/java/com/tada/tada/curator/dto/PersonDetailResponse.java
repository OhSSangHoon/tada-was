package com.tada.tada.curator.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/*
 * 사람 상세 모달의 헤더 + 상단 통계 + 한눈에 보기.
 *
 * 모달을 열 때 왕복을 한 번으로 끝내려고 세 영역을 한 응답에 담는다.
 * 타임라인과 추억 그룹은 페이징·상한이 달라 별도 API 다.
 *
 * `민수와의 기록` 같은 완성 문장은 내려주지 않는다. 조사는 프론트가 붙인다.
 * 날짜는 created_at 이 아니라 entry_date 기준이고,
 * ACTIVE 일기가 없으면 firstMentionedAt / lastMentionedAt 둘 다 null 이다.
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
