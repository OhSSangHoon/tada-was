package com.tada.tada.curator.dto;

import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
public class PersonMemoryDiaryResponse {

	private final UUID id;
	private final LocalDate entryDate;
	private final String title;

	public PersonMemoryDiaryResponse(
			UUID id,
			LocalDate entryDate,
			String title
	) {
		this.id = id;
		this.entryDate = entryDate;
		this.title = title;
	}
}