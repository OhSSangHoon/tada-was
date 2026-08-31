package com.tada.tada.auth.dto;

import lombok.Getter;

@Getter
public class AuthResponse {
	
	// 발급된 JWT
	private final String accessToken;
	
	public AuthResponse(String accessToken) {
		this.accessToken = accessToken;
	}
}