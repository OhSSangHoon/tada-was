package com.tada.tada.curator.dto;

import com.tada.tada.curator.entity.MentionEntityType;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class PersonMemoryGroupResponse {

	private final MentionEntityType groupType;
	private final String groupKey;
	private final LocalDate firstEntryDate;
	private final LocalDate lastEntryDate;
	private final long diaryCount;
	private final List<String> stickerUrls;
	private final List<PersonMemoryDiaryResponse> diaries;

	public PersonMemoryGroupResponse(
			MentionEntityType groupType,
			String groupKey,
			LocalDate firstEntryDate,
			LocalDate lastEntryDate,
			long diaryCount,
			List<String> stickerUrls,
			List<PersonMemoryDiaryResponse> diaries
	) {
		this.groupType = groupType;
		this.groupKey = groupKey;
		this.firstEntryDate = firstEntryDate;
		this.lastEntryDate = lastEntryDate;
		this.diaryCount = diaryCount;
		this.stickerUrls = stickerUrls;
		this.diaries = diaries;
	}
}