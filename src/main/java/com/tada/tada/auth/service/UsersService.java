package com.tada.tada.auth.service;

import com.tada.tada.auth.dto.AuthResponse;
import com.tada.tada.auth.dto.LoginForm;
import com.tada.tada.auth.dto.SignUpForm;
import com.tada.tada.auth.entity.Provider;
import com.tada.tada.auth.entity.RefreshToken;
import com.tada.tada.auth.entity.Users;
import com.tada.tada.auth.repository.RefreshTokenRepository;
import com.tada.tada.auth.repository.UsersRepository;
import com.tada.tada.global.exception.CustomException;
import com.tada.tada.global.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsersService {
	
	private final UsersRepository usersRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	
	@Value("${jwt.refresh-expiration}")
	private long refreshExpiration;
	
	// 일반 회원가입
	public void signUp(SignUpForm form) {
		
		// 비밀번호 확인
		if (!form.getPassword().equals(form.getPasswordConfirm())) {
			throw new CustomException("비밀번호가 일치하지 않습니다.", 400);
		}
		
		// LOCAL 계정 기준 아이디 중복 확인
		if (usersRepository
				.findByLoginIdAndProvider(
						form.getLoginId(),
						Provider.LOCAL
				)
				.isPresent()) {
			throw new CustomException("이미 사용 중인 아이디입니다.", 400);
		}
		
		// 비밀번호 암호화
		String encodedPassword =
				passwordEncoder.encode(form.getPassword());
		
		// 회원 생성
		Users users = new Users(
				form.getLoginId(),
				encodedPassword,
				form.getNickname()
		);
		
		// DB에 회원 저장
		usersRepository.save(users);
	}
	
	// 로그인
	@Transactional
	public AuthResponse login(LoginForm form) {
		
		// LOCAL 계정 조회
		Users users = usersRepository
				.findByLoginIdAndProvider(
						form.getLoginId(),
						Provider.LOCAL
				)
				.orElseThrow(() ->
						new CustomException(
								"아이디 또는 비밀번호가 일치하지 않습니다.",
								401
						));
		
		// 비밀번호 확인
		if (!passwordEncoder.matches(
				form.getPassword(),
				users.getPassword()
		)) {
			throw new CustomException(
					"아이디 또는 비밀번호가 일치하지 않습니다.",
					401
			);
		}
		
		// Access Token 발급
		String accessToken =
				jwtUtil.createToken(users.getId());
		
		// Refresh Token 발급
		String refreshToken =
				jwtUtil.createRefreshToken(users.getId());
		
		// 기존 Refresh Token 삭제
		refreshTokenRepository.deleteByUserId(users.getId());
		
		// Refresh Token 만료 시간 계산
		LocalDateTime expiresAt =
				LocalDateTime.now().plusNanos(
						refreshExpiration * 1_000_000
				);
		
		// Refresh Token 생성
		RefreshToken token = new RefreshToken(
				users.getId(),
				refreshToken,
				expiresAt
		);
		
		// Refresh Token DB 저장
		refreshTokenRepository.save(token);
		
		return new AuthResponse(
				accessToken,
				refreshToken
		);
	}
	
	// Refresh Token으로 Access Token 재발급
	@Transactional
	public AuthResponse reissue(String refreshToken) {
		
		// DB에 저장된 Refresh Token인지 확인
		RefreshToken savedToken =
				refreshTokenRepository
						.findByToken(refreshToken)
						.orElseThrow(() ->
								new CustomException(
										"유효하지 않은 Refresh Token입니다.",
										401
								));
		
		// JWT 자체가 유효한지 확인
		if (!jwtUtil.validateToken(refreshToken)) {
			refreshTokenRepository.deleteByToken(refreshToken);
			
			throw new CustomException(
					"만료되었거나 유효하지 않은 Refresh Token입니다.",
					401
			);
		}
		
		// DB에 저장된 만료 시간 확인
		if (savedToken
				.getExpiresAt()
				.isBefore(LocalDateTime.now())) {
			
			refreshTokenRepository.deleteByToken(refreshToken);
			
			throw new CustomException(
					"Refresh Token이 만료되었습니다.",
					401
			);
		}
		
		// Refresh Token에서 회원 ID 추출
		UUID userId =
				jwtUtil.getUserId(refreshToken);
		
		// DB의 회원 ID와 일치하는지 확인
		if (!savedToken.getUserId().equals(userId)) {
			throw new CustomException(
					"유효하지 않은 Refresh Token입니다.",
					401
			);
		}
		
		// 새로운 Access Token 발급
		String accessToken =
				jwtUtil.createToken(userId);
		
		// 기존 Refresh Token은 그대로 사용
		return new AuthResponse(
				accessToken,
				refreshToken
		);
	}
	
	// 로그아웃
	@Transactional
	public void logout(UUID userId) {
		
		// 해당 회원의 Refresh Token 삭제
		refreshTokenRepository.deleteByUserId(userId);
	}
}