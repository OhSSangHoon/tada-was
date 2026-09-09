package com.tada.tada.global.security;

import tools.jackson.databind.ObjectMapper;
import com.tada.tada.auth.dto.AuthResponse;
import com.tada.tada.auth.service.CustomOAuth2User;
import com.tada.tada.auth.service.RefreshTokenService;
import com.tada.tada.global.response.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
	
	private final JwtUtil jwtUtil;
	private final RefreshTokenService refreshTokenService;
	private final ObjectMapper objectMapper;
	
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
		
		// Refresh Token 생성 및 DB 저장
		refreshTokenService.saveRefreshToken(
				oAuth2User.getUserId(),
				refreshToken
		);
		
		// Access Token + Refresh Token 응답
		AuthResponse authResponse =
				new AuthResponse(accessToken, refreshToken);
		
		ApiResponse<AuthResponse> responseBody =
				ApiResponse.success(authResponse);
		
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		
		// ApiResponse를 JSON으로 변환해서 응답
		response.getWriter().write(
				objectMapper.writeValueAsString(responseBody)
		);
	}
}