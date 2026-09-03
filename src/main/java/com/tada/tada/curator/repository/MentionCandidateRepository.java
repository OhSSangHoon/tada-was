package com.tada.tada.curator.repository;

import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.entity.MentionCandidateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MentionCandidateRepository
		extends JpaRepository<MentionCandidate, UUID> {

	List<MentionCandidate> findAllByDiaryId(
			UUID diaryId
	);

	boolean existsByDiaryId(
			UUID diaryId
	);
	
	@Query("""
        SELECT candidate
        FROM MentionCandidate candidate, Diary diary
        WHERE candidate.diaryId = diary.id
          AND diary.userId = :userId
          AND candidate.entityType = 'PERSON'
          AND candidate.status = :status
          AND candidate.matchedPersonId IS NOT NULL
          AND (
                candidate.rawText = :rawText
                OR candidate.normalizedText = :normalizedText
          )
        ORDER BY diary.entryDate DESC, candidate.id ASC
        """)
	List<MentionCandidate> findPersonMatchHistory(
			@Param("userId") UUID userId,
			@Param("rawText") String rawText,
			@Param("normalizedText") String normalizedText,
			@Param("status") MentionCandidateStatus status
	);
}