package com.tada.tada.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

// Spring Security 체인 전용 필터라 스프링 빈(@Component)으로 만들지 않는다.
// 빈으로 등록하면 Spring Boot가 전역 서블릿 필터로도 자동 등록해버려서
// SecurityConfig의 addFilterBefore() 등록과 겹쳐 요청마다 두 번 실행된다.
// 대신 SecurityConfig에서 필요한 의존성(JwtUtil)만 받아 직접 new로 생성한다.
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