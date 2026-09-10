package com.tada.tada.curator.dto;

import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/*
 * aliases는 화면에 안 그리고 클라이언트 필터용이다.
 * stickerUrl이 null이면 프론트가 기본 이미지를 그린다.
 */
@Getter
public class PersonSummaryResponse {

	private final UUID id;
	private final String displayName;
	private final List<String> aliases;
	private final int mentionCount;
	private final LocalDate lastMentionedAt;
	private final String stickerUrl;

	public PersonSummaryResponse(
			UUID id,
			String displayName,
			List<String> aliases,
			int mentionCount,
			LocalDate lastMentionedAt,
			String stickerUrl
	) {
		this.id = id;
		this.displayName = displayName;
		this.aliases = aliases;
		this.mentionCount = mentionCount;
		this.lastMentionedAt = lastMentionedAt;
		this.stickerUrl = stickerUrl;
	}
}
