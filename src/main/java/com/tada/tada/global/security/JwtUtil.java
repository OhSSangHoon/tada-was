package com.tada.tada.global.security;

import io.jsonwebtoken.Claims;
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
	
	// JWT 생성
	public String createToken(UUID userId) {
		Date now = new Date();
		
		return Jwts.builder()
				.subject(userId.toString())
				.issuedAt(now)
				.expiration(new Date(now.getTime() + expiration))
				.signWith(getKey())
				.compact();
	}
	
	// JWT에서 회원 ID 확인
	public UUID getUserId(String token) {
		String subject = Jwts.parser()
				.verifyWith(getKey())
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.getSubject();
		
		return UUID.fromString(subject);
	}
	
	// JWT 유효성 확인
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
	
	// 비밀키 생성
	private SecretKey getKey() {
		return Keys.hmacShaKeyFor(
				secret.getBytes(StandardCharsets.UTF_8)
		);
	}
}