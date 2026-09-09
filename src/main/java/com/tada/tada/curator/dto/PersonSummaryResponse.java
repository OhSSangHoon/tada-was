package com.tada.tada.curator.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/*
 * aliases는 화면에 안 그리고 클라이언트 필터용이다.
 * stickerUrl이 null이면 프론트가 기본 이미지를 그린다.
 */
public record PersonSummaryResponse(
		UUID id,
		String displayName,
		List<String> aliases,
		int mentionCount,
		LocalDate lastMentionedAt,
		String stickerUrl
) {
}
