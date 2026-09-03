package com.tada.tada.curator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@IdClass(DiaryPersonId.class)
@Table(name = "diary_person")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryPerson {

	@Id
	@Column(name = "diary_id", nullable = false)
	private UUID diaryId;

	@Id
	@Column(name = "person_id", nullable = false)
	private UUID personId;

	public static DiaryPerson create(
			UUID diaryId,
			UUID personId
	) {
		if (diaryId == null) {
			throw new IllegalArgumentException(
					"diaryId must not be null"
			);
		}

		if (personId == null) {
			throw new IllegalArgumentException(
					"personId must not be null"
			);
		}

		DiaryPerson diaryPerson =
				new DiaryPerson();

		diaryPerson.diaryId = diaryId;
		diaryPerson.personId = personId;

		return diaryPerson;
	}
}