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
@Table(name = "memory_person")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemoryPerson {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "display_name", nullable = false)
	private String displayName;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	public static MemoryPerson create(
			UUID userId,
			String displayName
	) {
		if (userId == null) {
			throw new IllegalArgumentException(
					"userId must not be null"
			);
		}

		if (displayName == null
				|| displayName.isBlank()) {
			throw new IllegalArgumentException(
					"displayName must not be blank"
			);
		}

		MemoryPerson person =
				new MemoryPerson();

		person.id = UUID.randomUUID();
		person.userId = userId;
		person.displayName = displayName;
		person.createdAt = LocalDateTime.now();

		return person;
	}
}