package com.tada.tada.curator.repository;

import com.tada.tada.curator.entity.MentionCandidatePersonRef;
import com.tada.tada.curator.entity.MentionCandidatePersonRefId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MentionCandidatePersonRefRepository
		extends JpaRepository<
		MentionCandidatePersonRef,
		MentionCandidatePersonRefId
		> {

	List<MentionCandidatePersonRef> findAllBySourceCandidateId(
			UUID sourceCandidateId
	);

	List<MentionCandidatePersonRef> findAllBySourceCandidateIdIn(
			Collection<UUID> sourceCandidateIds
	);

	List<MentionCandidatePersonRef> findAllByPersonCandidateId(
			UUID personCandidateId
	);
}