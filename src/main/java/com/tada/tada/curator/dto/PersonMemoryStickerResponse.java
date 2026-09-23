package com.tada.tada.curator.dto;

import lombok.Getter;

@Getter
public class PersonMemoryStickerResponse {

	private final String imageUrl;
	private final String keyword;

	public PersonMemoryStickerResponse(
			String imageUrl,
			String keyword
	) {
		this.imageUrl = imageUrl;
		this.keyword = keyword;
	}
}