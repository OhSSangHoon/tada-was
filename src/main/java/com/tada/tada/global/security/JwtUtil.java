package com.tada.tada.global.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
	
	@Value("${jwt.secret}")
	private String secret;
	
	@Value("${jwt.expiration}")
	private long expiration;
	
	@Value("${jwt.refresh-expiration}")
	private long refreshExpiration;
	
	// Access Token 생성
	public String createToken(UUID userId) {
		Date now = new Date();
		
		return Jwts.builder()
				.subject(userId.toString())
				.claim("type", "access")
				.issuedAt(now)
				.expiration(new Date(now.getTime() + expiration))
				.signWith(getKey())
				.compact();
	}
	
	// Refresh Token 생성
	public String createRefreshToken(UUID userId) {
		Date now = new Date();
		
		return Jwts.builder()
				.subject(userId.toString())
				.claim("type", "refresh")
				.issuedAt(now)
				.expiration(new Date(now.getTime() + refreshExpiration))
				.signWith(getKey())
				.compact();
	}
	
	// JWT에서 사용자 ID 추출
	public UUID getUserId(String token) {
		String subject = Jwts.parser()
				.verifyWith(getKey())
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.getSubject();
		
		return UUID.fromString(subject);
	}
	
	// JWT 자체의 유효성 확인
	public boolean validateToken(String token) {
		try {
			Jwts.parser()
					.verifyWith(getKey())
					.build()
					.parseSignedClaims(token);
			
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	// Access Token인지 확인
	public boolean isAccessToken(String token) {
		try {
			String type = Jwts.parser()
					.verifyWith(getKey())
					.build()
					.parseSignedClaims(token)
					.getPayload()
					.get("type", String.class);
			
			return "access".equals(type);
		} catch (Exception e) {
			return false;
		}
	}
	
	private SecretKey getKey() {
		return Keys.hmacShaKeyFor(
				secret.getBytes(StandardCharsets.UTF_8)
		);
	}
}