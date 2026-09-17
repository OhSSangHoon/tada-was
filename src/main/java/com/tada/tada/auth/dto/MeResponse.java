package com.tada.tada.auth.dto;

import lombok.Getter;

import java.util.UUID;

@Getter
public class MeResponse {
	private final UUID id;
	private final String nickname;
	
	public MeResponse(UUID id, String nickname) {
		this.id = id;
		this.nickname = nickname;
	}
}
