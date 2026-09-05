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

	/*
	 * Diary 영구삭제 때 Diary 도메인이 CuratorCleanupService 를 통해 호출한다.
	 *
	 * mention_candidate_person_ref 는 두 FK 가 ON DELETE CASCADE 이므로
	 * 여기서 Candidate 를 지우면 DB 가 함께 정리한다.
	 * 같은 Relation 을 서비스에서 다시 지우지 않는다.
	 */
	void deleteByDiaryId(
			UUID diaryId
	);
	
	@Query("""
        SELECT candidate
        FROM MentionCandidate candidate, Diary diary
        WHERE candidate.diaryId = diary.id
          AND diary.userId = :userId
          AND candidate.entityType = com.tada.tada.curator.entity.MentionEntityType.PERSON
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