package com.tada.tada.curator.dto;

import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
public class MemoryRecallResponse {

	private final MemoryRecallType eventType;
	private final String message;
	private final UUID diaryId;
	private final LocalDate entryDate;
	private final String title;
	private final String contentPreview;
	private final String stickerUrl;
	private final List<String> tags;

	public MemoryRecallResponse(
			MemoryRecallType eventType,
			String message,
			UUID diaryId,
			LocalDate entryDate,
			String title,
			String contentPreview,
			String stickerUrl,
			List<String> tags
	) {
		this.eventType = eventType;
		this.message = message;
		this.diaryId = diaryId;
		this.entryDate = entryDate;
		this.title = title;
		this.contentPreview = contentPreview;
		this.stickerUrl = stickerUrl;
		this.tags = tags;
	}
}