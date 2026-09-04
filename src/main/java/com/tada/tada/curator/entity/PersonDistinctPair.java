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
@Table(name = "person_distinct_pair")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonDistinctPair {
	
	@Id
	private UUID id;
	
	@Column(name = "person_id_a", nullable = false)
	private UUID personIdA;
	
	@Column(name = "person_id_b", nullable = false)
	private UUID personIdB;
	
	@Column(name = "confirmed_at", nullable = false)
	private LocalDateTime confirmedAt;
}