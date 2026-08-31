package com.tada.tada.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
	
	private final JwtUtil jwtUtil;
	
	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		
		// 요청 헤더에서 JWT 가져오기
		String authorization = request.getHeader("Authorization");
		
		// JWT가 없으면 다음 필터로 넘어감
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		
		// "Bearer " 뒤의 실제 토큰만 추출
		String token = authorization.substring(7);
		
		// JWT가 유효한 경우
		if (jwtUtil.validateToken(token)) {
			UUID userId = jwtUtil.getUserId(token);
			
			// 현재 요청의 인증 정보 저장
			SecurityContextHolder.getContext().setAuthentication(
					new JwtAuthentication(userId)
			);
		}
		
		filterChain.doFilter(request, response);
	}
}