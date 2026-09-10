package com.tada.tada.curator.dto;

import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/*
 * 헤더+통계+한눈에 보기를 1회 응답으로 묶는다 (타임라인·추억 그룹은 별도 API).
 * displayName은 조사 없는 원형(문장 조합은 프론트), 날짜는 ACTIVE 일기 없으면 null.
 */
@Getter
public class PersonDetailResponse {

	private final UUID id;
	private final String displayName;
	private final String stickerUrl;
	private final int mentionCount;
	private final LocalDate firstMentionedAt;
	private final LocalDate lastMentionedAt;
	private final List<PersonEntityStatResponse> topPlaces;
	private final List<PersonEntityStatResponse> topActivities;

	public PersonDetailResponse(
			UUID id,
			String displayName,
			String stickerUrl,
			int mentionCount,
			LocalDate firstMentionedAt,
			LocalDate lastMentionedAt,
			List<PersonEntityStatResponse> topPlaces,
			List<PersonEntityStatResponse> topActivities
	) {
		this.id = id;
		this.displayName = displayName;
		this.stickerUrl = stickerUrl;
		this.mentionCount = mentionCount;
		this.firstMentionedAt = firstMentionedAt;
		this.lastMentionedAt = lastMentionedAt;
		this.topPlaces = topPlaces;
		this.topActivities = topActivities;
	}
}
