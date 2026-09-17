package com.tada.tada.curator.dto;

import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
public class PersonMemoryDiaryResponse {

	private final UUID diaryId;
	private final LocalDate entryDate;
	private final String title;

	public PersonMemoryDiaryResponse(
			UUID diaryId,
			LocalDate entryDate,
			String title
	) {
		this.diaryId = diaryId;
		this.entryDate = entryDate;
		this.title = title;
	}
}