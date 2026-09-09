package com.tada.tada.auth.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class CustomOAuth2User implements OAuth2User {
	
	private final UUID userId;
	private final Map<String, Object> attributes;
	
	// 우리 DB의 회원 ID와 소셜 로그인 사용자 정보를 저장한다.
	public CustomOAuth2User(
			UUID userId,
			Map<String, Object> attributes
	) {
		this.userId = userId;
		this.attributes = attributes;
	}
	
	// 현재는 별도의 권한(Role)을 사용하지 않는다.
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Collections.emptyList();
	}
	
	// 소셜 로그인에서 받은 사용자 정보를 반환한다.
	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}
	
	// Spring Security가 사용하는 사용자 이름을 반환한다.
	@Override
	public String getName() {
		return userId.toString();
	}
	
	// 우리 DB의 회원 ID를 가져온다.
	public UUID getUserId() {
		return userId;
	}
}