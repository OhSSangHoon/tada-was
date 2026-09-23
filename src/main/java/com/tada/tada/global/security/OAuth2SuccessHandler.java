package com.tada.tada.global.security;

import tools.jackson.databind.ObjectMapper;
import com.tada.tada.auth.dto.AuthResponse;
import com.tada.tada.auth.service.CustomOAuth2User;
import com.tada.tada.auth.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
	
	// 로컬에서는 localhost:3000,
	// 배포 환경에서는 FRONTEND_URL 환경변수를 사용한다.
	@Value("${FRONTEND_URL:http://localhost:3000}")
	private String frontendUrl;
	
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
		
		// 프론트에 전달할 인증 정보를 JSON으로 변환한다.
		String authData =
				objectMapper.writeValueAsString(authResponse);
		
		// postMessage의 targetOrigin도 JSON 문자열로 변환한다.
		String frontendOrigin =
				objectMapper.writeValueAsString(frontendUrl);
		
		// 소셜 로그인 성공 정보를 부모 창으로 전달하고 팝업을 닫는다.
		String html = """
             <!DOCTYPE html>
             <html>
             <body>
             <script>
                if (window.opener) {
                   window.opener.postMessage(
                      {
                         type: "oauth-success",
                         auth: %s
                      },
                      %s
                   );
                }

                window.close();
             </script>
             </body>
             </html>
             """.formatted(authData, frontendOrigin);
		
		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(html);
	}
}