package com.tada.tada.global.security;

import com.tada.tada.auth.dto.AuthResponse;
import com.tada.tada.auth.entity.RefreshToken;
import com.tada.tada.auth.repository.RefreshTokenRepository;
import com.tada.tada.auth.service.CustomOAuth2User;
import com.tada.tada.global.response.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
	
	private final JwtUtil jwtUtil;
	private final RefreshTokenRepository refreshTokenRepository;
	
	@Value("${jwt.refresh-expiration}")
	private long refreshExpiration;
	
	@Transactional
	@Override
	public void onAuthenticationSuccess(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication
	) throws IOException, ServletException {
		
		// 소셜 로그인으로 인증된 사용자 정보
		CustomOAuth2User oAuth2User =
				(CustomOAuth2User) authentication.getPrincipal();
		
		// Access Token 발급
		String accessToken =
				jwtUtil.createToken(oAuth2User.getUserId());
		
		// Refresh Token 발급
		String refreshToken =
				jwtUtil.createRefreshToken(oAuth2User.getUserId());
		
		// 기존 Refresh Token 삭제
		refreshTokenRepository.deleteByUserId(
				oAuth2User.getUserId()
		);
		
		// Refresh Token 만료 시간 계산
		LocalDateTime expiresAt =
				LocalDateTime.now().plusNanos(
						refreshExpiration * 1_000_000
				);
		
		// Refresh Token DB 저장
		RefreshToken token = new RefreshToken(
				oAuth2User.getUserId(),
				refreshToken,
				expiresAt
		);
		
		refreshTokenRepository.save(token);
		
		// Access Token + Refresh Token 응답
		AuthResponse authResponse =
				new AuthResponse(accessToken, refreshToken);
		
		ApiResponse<AuthResponse> responseBody =
				ApiResponse.success(authResponse);
		
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		
		response.getWriter().write(
				"{\"success\":true,\"data\":{\"accessToken\":\""
						+ accessToken
						+ "\",\"refreshToken\":\""
						+ refreshToken
						+ "\"},\"message\":null}"
		);
	}
}