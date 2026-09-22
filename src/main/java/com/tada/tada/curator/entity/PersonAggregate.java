package com.tada.tada.curator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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

	public static PersonAggregate create(
			UUID personId,
			int mentionCount,
			LocalDate lastMentionedDate
	) {
		if (personId == null) {
			throw new IllegalArgumentException(
					"personId must not be null"
			);
		}

		PersonAggregate aggregate =
				new PersonAggregate();

		aggregate.personId = personId;
		aggregate.applySnapshot(
				mentionCount,
				lastMentionedDate
		);

		return aggregate;
	}

	public void applySnapshot(
			int mentionCount,
			LocalDate lastMentionedDate
	) {
		if (mentionCount < 0) {
			throw new IllegalArgumentException(
					"mentionCount must not be negative"
			);
		}

		if (mentionCount == 0
				&& lastMentionedDate != null) {
			throw new IllegalArgumentException(
					"lastMentionedDate must be null when mentionCount is zero"
			);
		}

		if (mentionCount > 0
				&& lastMentionedDate == null) {
			throw new IllegalArgumentException(
					"lastMentionedDate must not be null when mentionCount is positive"
			);
		}

		this.mentionCount =
				mentionCount;

		this.lastMentionedAt =
				lastMentionedDate == null
						? null
						: lastMentionedDate.atStartOfDay();
	}
}