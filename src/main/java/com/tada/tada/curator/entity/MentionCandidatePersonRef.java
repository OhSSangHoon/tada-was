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
@IdClass(MentionCandidatePersonRefId.class)
@Table(name = "mention_candidate_person_ref")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MentionCandidatePersonRef {

	@Id
	@Column(name = "source_candidate_id", nullable = false)
	private UUID sourceCandidateId;

	@Id
	@Column(name = "person_candidate_id", nullable = false)
	private UUID personCandidateId;

	public static MentionCandidatePersonRef create(
			UUID sourceCandidateId,
			UUID personCandidateId
	) {
		if (sourceCandidateId == null) {
			throw new IllegalArgumentException(
					"sourceCandidateId must not be null"
			);
		}

		if (personCandidateId == null) {
			throw new IllegalArgumentException(
					"personCandidateId must not be null"
			);
		}

		if (sourceCandidateId.equals(personCandidateId)) {
			throw new IllegalArgumentException(
					"sourceCandidateId and personCandidateId must be different"
			);
		}

		MentionCandidatePersonRef relation =
				new MentionCandidatePersonRef();

		relation.sourceCandidateId =
				sourceCandidateId;

		relation.personCandidateId =
				personCandidateId;

		return relation;
	}
}