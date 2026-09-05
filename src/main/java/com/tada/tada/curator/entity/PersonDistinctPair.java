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
	 * 실제 DB 에는 쌍 unique 도 self-pair CHECK 도 없다. (명세 7.8)
	 * 따라서 canonical 순서와 self-pair 금지를 여기서 강제한다.
	 *
	 * person_id_a < person_id_b 로 고정해
	 * (A,B) 와 (B,A) 가 서로 다른 행으로 중복 저장되는 것을 막는다.
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
