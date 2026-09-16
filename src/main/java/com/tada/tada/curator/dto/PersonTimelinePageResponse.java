package com.tada.tada.curator.dto;

import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class PersonTimelinePageResponse {

	private final List<PersonTimelineItemResponse> items;
	private final LocalDate nextCursor;

	public PersonTimelinePageResponse(
			List<PersonTimelineItemResponse> items,
			LocalDate nextCursor
	) {
		this.items = items;
		this.nextCursor = nextCursor;
	}
}