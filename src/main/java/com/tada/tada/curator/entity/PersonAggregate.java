package com.tada.tada.curator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "person_aggregate")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonAggregate {
	
	@Id
	@Column(name = "person_id", nullable = false)
	private UUID personId;
	
	@Column(name = "mention_count", nullable = false)
	private int mentionCount;
	
	@Column(name = "last_mentioned_at")
	private LocalDateTime lastMentionedAt;
}