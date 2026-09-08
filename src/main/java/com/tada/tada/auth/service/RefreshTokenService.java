package com.tada.tada.auth.service;

import com.tada.tada.auth.entity.RefreshToken;
import com.tada.tada.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
	
	private final RefreshTokenRepository refreshTokenRepository;
	
	@Value("${jwt.refresh-expiration}")
	private long refreshExpiration;
	
	// 기존 Refresh Token을 삭제하고 새로운 Refresh Token을 저장한다.
	@Transactional
	public void saveRefreshToken(UUID userId, String refreshToken) {
		
		refreshTokenRepository.deleteByUserId(userId);
		
		LocalDateTime expiresAt =
				LocalDateTime.now()
						.plus(Duration.ofMillis(refreshExpiration));
		
		RefreshToken token = new RefreshToken(
				userId,
				refreshToken,
				expiresAt
		);
		
		refreshTokenRepository.save(token);
	}
}