package com.tada.tada.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor
public class RefreshToken {
	
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@Column(name = "user_id", nullable = false)
	private UUID userId;
	
	@Column(name = "token", nullable = false)
	private String token;
	
	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;
	
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
	
	public RefreshToken(
			UUID userId,
			String token,
			LocalDateTime expiresAt
	) {
		this.userId = userId;
		this.token = token;
		this.expiresAt = expiresAt;
		this.createdAt = LocalDateTime.now();
	}
}