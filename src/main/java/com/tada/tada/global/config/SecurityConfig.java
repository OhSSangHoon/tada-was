package com.tada.tada.global.config;

import com.tada.tada.auth.service.CustomOAuth2UserService;
import com.tada.tada.global.security.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.tada.tada.global.security.JwtAuthFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.util.List;

/**
 * Spring Security 전역 설정.
 *
 * [담당: 상훈] — CORS, 인가(어떤 API를 로그인 없이 열어둘지) 규칙 담당
 * [담당: 진경] — JWT 필터 직접 작성 후, 아래 filterChain()에 등록하는 한 줄만 추가
 *
 * 여기서 하는 일:
 *   1. CORS 설정 — 프론트(Next.js)가 이 서버로 요청 보낼 수 있게 허용
 *   2. 어떤 API는 로그인 없이 접근 가능하고, 어떤 API는 토큰 필요한지 규칙 정의
 *   3. (진경이 작성할 부분) JwtAuthFilter를 실제 요청 처리 흐름에 등록
 *
 * 팀원들이 새 도메인 API를 추가할 때, 로그인 없이 접근 가능해야 하는 API가 있으면
 * 아래 permitAll() 목록에 경로를 추가해달라고 상훈한테 요청할 것.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	
	private final JwtAuthFilter jwtAuthFilter;
	
	// 소셜 로그인으로 전달받은 사용자 정보를 처리한다.
	private final CustomOAuth2UserService customOAuth2UserService;
	
	// 소셜 로그인 성공 후 우리 서비스의 JWT를 발급한다.
	private final OAuth2SuccessHandler oAuth2SuccessHandler;
	
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				// REST API 서버라 세션을 안 쓰므로 CSRF 보호 불필요 (JWT로 대체)
				.csrf(csrf -> csrf.disable())

				// 세션을 아예 안 만들게 설정 — 매 요청마다 JWT로만 인증 판단
				.sessionManagement(session ->
						session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// CORS 설정 적용 (아래 corsConfigurationSource() 참고)
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))

				.authorizeHttpRequests(auth -> auth
						// 헬스체크용 루트 경로 — 서버 떠있는지 확인용
						.requestMatchers("/").permitAll()
						
						// Swagger 문서는 인증 없이 누구나 볼 수 있어야 함
						.requestMatchers(
								"/swagger-ui/**",
								"/v3/api-docs/**"
						).permitAll()

						// 회원가입/로그인은 당연히 토큰 없이도 호출 가능해야 함
						.requestMatchers("/api/auth/**").permitAll()

						// n8n이 콜백 보내는 내부 전용 경로 — X-Internal-Secret 헤더로 별도 인증 예정
						.requestMatchers("/api/internal/**").permitAll()
						
						// OAuth2 소셜 로그인 시작 및 콜백 경로는 인증 없이 접근 가능
						.requestMatchers(
								"/oauth2/**",
								"/login/**"
						).permitAll()
						
						// 게스트도 볼 수 있는 화면이 있다면 여기 추가
						// 예: .requestMatchers("/api/guest/**").permitAll()
	
						// 그 외 모든 API는 로그인(유효한 Access Token) 필요
						.anyRequest().authenticated()
				)
				
				// OAuth2 소셜 로그인 설정
				.oauth2Login(oauth2 -> oauth2
								
								// Google/Kakao에서 받은 사용자 정보를
								// CustomOAuth2UserService에서 처리한다.
								.userInfoEndpoint(userInfo -> userInfo
										.userService(customOAuth2UserService)
								)
								
								// 소셜 로그인 성공 후 우리 서비스의 JWT를 발급한다.
								.successHandler(oAuth2SuccessHandler)
				);

		// TODO(진경/B): 여기에 JwtAuthFilter 등록 필요
		// 본인이 만든 JwtAuthFilter를 아래처럼 등록하면 됨:
		//   http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
		
		// JWT 필터를 Spring Security의 기본 인증 필터보다 먼저 실행
		// 요청에 들어온 Access Token을 확인해서 로그인 여부를 판단한다.
		http.addFilterBefore(
				jwtAuthFilter,
				UsernamePasswordAuthenticationFilter.class
		);
		
		// 이렇게 하려면 이 클래스에 생성자 주입 추가 필요:
		//   @RequiredArgsConstructor 어노테이션 추가 +
		//   private final JwtAuthFilter jwtAuthFilter; 필드 추가

		return http.build();
	}

	/**
	 * CORS(Cross-Origin Resource Sharing) 설정.
	 * 프론트(Next.js)와 백엔드가 다른 도메인이라, 이 설정이 없으면
	 * 브라우저가 보안상 요청 자체를 막아버린다.
	 *
	 * TODO(상훈): Vercel 배포 완료되면 실제 배포 주소를 allowedOrigins에 추가할 것.
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		configuration.setAllowedOrigins(List.of(
				"http://localhost:3000",  // 로컬 프론트 개발 주소
				"https://tada-frontend-seven.vercel.app" // 배포 프론트 개발 주소
		));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);  // 쿠키(Refresh Token) 주고받으려면 필수

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	/**
	 * 비밀번호 암호화에 쓸 인코더.
	 * 진경이 회원가입 시 passwordEncoder.encode(rawPassword)로 암호화해서 저장하고,
	 * 로그인 시 passwordEncoder.matches(입력값, 저장된값)으로 비교하면 됨.
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}