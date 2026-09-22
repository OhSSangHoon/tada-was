package com.tada.tada.curator.dto;

import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
public class PersonTimelineItemResponse {

	private final UUID diaryId;
	private final List<UUID> personCandidateIds;
	private final LocalDate entryDate;
	private final String title;
	private final String stickerUrl;
	private final List<String> keywords;

	public PersonTimelineItemResponse(
			UUID diaryId,
			List<UUID> personCandidateIds,
			LocalDate entryDate,
			String title,
			String stickerUrl,
			List<String> keywords
	) {
		this.diaryId = diaryId;
		this.personCandidateIds = personCandidateIds;
		this.entryDate = entryDate;
		this.title = title;
		this.stickerUrl = stickerUrl;
		this.keywords = keywords;
	}
}