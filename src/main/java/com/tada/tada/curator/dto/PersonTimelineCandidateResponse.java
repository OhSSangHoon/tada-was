package com.tada.tada.curator.dto;

import lombok.Getter;

import java.util.UUID;

@Getter
public class PersonTimelineCandidateResponse {

	private final UUID id;
	private final String rawText;

	public PersonTimelineCandidateResponse(
			UUID id,
			String rawText
	) {
		this.id = id;
		this.rawText = rawText;
	}
}