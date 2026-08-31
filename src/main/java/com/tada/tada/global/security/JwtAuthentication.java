package com.tada.tada.global.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.List;
import java.util.UUID;

public class JwtAuthentication extends AbstractAuthenticationToken {
	
	private final UUID userId;
	
	// JWT에서 확인한 회원 ID를 인증 정보로 저장
	public JwtAuthentication(UUID userId) {
		super(List.of()); // 현재는 별도의 권한(Role)을 사용하지 않음
		this.userId = userId;
		setAuthenticated(true);
	}
	
	// 현재 로그인한 회원 ID
	@Override
	public Object getPrincipal() {
		return userId;
	}
	
	// 비밀번호 같은 인증 정보는 사용하지 않음
	@Override
	public Object getCredentials() {
		return null;
	}
}