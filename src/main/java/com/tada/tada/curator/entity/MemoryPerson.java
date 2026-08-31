package com.tada.tada.curator.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "memory_person")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemoryPerson {
	
	@Id
	private UUID id;
	
	@Column(name = "user_id",  nullable = false)
	private UUID userId;
	
	@Column(name = "display_name", nullable = false)
	private String displayName;
	
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
}
