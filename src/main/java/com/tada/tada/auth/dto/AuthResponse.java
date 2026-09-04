package com.tada.tada.auth.dto;

import lombok.Getter;

@Getter
public class AuthResponse {
	
	// 발급된 Access Token
	private final String accessToken;
	
	// 발급된 Refresh Token
	private final String refreshToken;
	
	public AuthResponse(
			String accessToken,
			String refreshToken
	) {
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
	}
}