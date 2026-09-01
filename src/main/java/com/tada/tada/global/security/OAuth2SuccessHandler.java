package com.tada.tada.global.security;

import com.tada.tada.auth.dto.AuthResponse;
import com.tada.tada.auth.service.CustomOAuth2User;
import com.tada.tada.global.response.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 소셜 로그인 성공 후 JWT를 발급한다.
 */
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
	
	private final JwtUtil jwtUtil;
	
	@Override
	public void onAuthenticationSuccess(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication
	) throws IOException, ServletException {
		
		// 소셜 로그인으로 인증된 우리 서비스 사용자 정보를 가져온다.
		CustomOAuth2User oAuth2User =
				(CustomOAuth2User) authentication.getPrincipal();
		
		// 우리 DB의 회원 ID를 이용해서 Access Token을 발급한다.
		String accessToken =
				jwtUtil.createToken(oAuth2User.getUserId());
		
		// 기존 일반 로그인과 동일한 응답 DTO를 사용한다.
		AuthResponse authResponse =
				new AuthResponse(accessToken);
		
		// 기존 API 응답 형식으로 감싼다.
		ApiResponse<AuthResponse> responseBody =
				ApiResponse.success(authResponse);
		
		// JSON 응답 설정
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		
		// 발급한 Access Token을 응답한다.
		response.getWriter().write(
				"{\"success\":true,\"data\":{\"accessToken\":\""
						+ accessToken
						+ "\"},\"message\":null}"
		);
	}
}