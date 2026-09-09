package com.tada.tada.curator.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/*
 * 홈 사람 카드 한 장.
 *
 * aliases 는 화면에 그리지 않는다. 검색 API 를 따로 두지 않고
 * 이 배열을 받아 클라이언트가 이름 + 별칭으로 필터한다.
 * stickerUrl 이 null 이면 프론트가 기본 이미지를 그린다.
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
