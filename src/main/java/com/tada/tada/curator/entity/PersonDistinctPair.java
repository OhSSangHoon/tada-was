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

	/*
	 * DB에 쌍 unique·self-pair CHECK가 없어(명세 7.8) 여기서 강제한다.
	 * person_id_a < person_id_b 로 고정해 (A,B)/(B,A) 중복 저장을 막는다.
	 */
	public static PersonDistinctPair create(
			UUID personIdA,
			UUID personIdB
	) {
		if (personIdA == null
				|| personIdB == null) {
			throw new IllegalArgumentException(
					"personId must not be null"
			);
		}

		if (personIdA.equals(personIdB)) {
			throw new IllegalArgumentException(
					"self pair is not allowed"
			);
		}

		PersonDistinctPair pair =
				new PersonDistinctPair();

		boolean aIsFirst =
				personIdA.toString()
						.compareTo(
								personIdB.toString()
						) < 0;

		pair.id = UUID.randomUUID();
		pair.personIdA = aIsFirst ? personIdA : personIdB;
		pair.personIdB = aIsFirst ? personIdB : personIdA;
		pair.confirmedAt = LocalDateTime.now();

		return pair;
	}
}
