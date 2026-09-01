package com.tada.tada.curator.repository;

import com.tada.tada.curator.entity.MentionCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MentionCandidateRepository
		extends JpaRepository<MentionCandidate, UUID> {
	
	List<MentionCandidate> findAllByDiaryId(UUID diaryId);
}