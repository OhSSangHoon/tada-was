package com.tada.tada.curator.repository;

import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.entity.MentionCandidateStatus;
import com.tada.tada.curator.entity.MentionEntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface MentionCandidateRepository
		extends JpaRepository<MentionCandidate, UUID> {

	List<MentionCandidate> findAllByDiaryId(
			UUID diaryId
	);

	boolean existsByDiaryId(
			UUID diaryId
	);

	/*
	 * Diary 영구삭제 시 호출한다. mention_candidate_person_ref는 FK ON DELETE CASCADE로
	 * 함께 정리되므로 서비스에서 중복 삭제하지 않는다.
	 */
	void deleteByDiaryId(
			UUID diaryId
	);
	
	/*
	 * PersonCreationGuard 재사용 판단용 이력 조회. diary.status=ACTIVE 필터가 없으면
	 * 휴지통 일기의 우연한 표현까지 이력에 섞여 재사용 여부를 좌우하게 된다.
	 */
	@Query("""
        SELECT candidate
        FROM MentionCandidate candidate, Diary diary
        WHERE candidate.diaryId = diary.id
          AND diary.userId = :userId
          AND diary.status = com.tada.tada.diary.entity.DiaryStatus.ACTIVE
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

	/*
	 * 그 사람과 함께한 장소·활동 집계. CONFIRMED PERSON -> ref -> source 경로만 써서
	 * 혼자 한 활동까지 포함되는 것을 막는다. COUNT(DISTINCT source.diaryId)로 일기당 1회로 세고,
	 * normalizedText까지 정렬해 동률 시 비결정성을 없앤다. Top3·타임라인·추억 그룹이 공용으로 쓴다.
	 */
	@Query("""
			SELECT
				source.entityType AS entityType,
				source.normalizedText AS normalizedText,
				COUNT(DISTINCT source.diaryId) AS diaryCount,
				MIN(diary.entryDate) AS firstEntryDate,
				MAX(diary.entryDate) AS lastEntryDate
			FROM MentionCandidate personCandidate,
				 MentionCandidatePersonRef relation,
				 MentionCandidate source,
				 Diary diary
			WHERE personCandidate.entityType = com.tada.tada.curator.entity.MentionEntityType.PERSON
			  AND personCandidate.status = com.tada.tada.curator.entity.MentionCandidateStatus.CONFIRMED
			  AND personCandidate.matchedPersonId = :personId
			  AND relation.personCandidateId = personCandidate.id
			  AND source.id = relation.sourceCandidateId
			  AND source.diaryId = personCandidate.diaryId
			  AND source.entityType IN (
					com.tada.tada.curator.entity.MentionEntityType.PLACE,
					com.tada.tada.curator.entity.MentionEntityType.ACTIVITY
			  )
			  AND diary.id = source.diaryId
			  AND diary.userId = :userId
			  AND diary.status = com.tada.tada.diary.entity.DiaryStatus.ACTIVE
			GROUP BY source.entityType, source.normalizedText
			ORDER BY COUNT(DISTINCT source.diaryId) DESC,
					 MAX(diary.entryDate) DESC,
					 source.normalizedText ASC
			""")
	List<PersonEntityStat> findPersonEntityStats(
			@Param("userId") UUID userId,
			@Param("personId") UUID personId
	);

	interface PersonEntityStat {

		MentionEntityType getEntityType();

		String getNormalizedText();

		long getDiaryCount();

		LocalDate getFirstEntryDate();

		LocalDate getLastEntryDate();
	}
	
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		SELECT candidate
		FROM MentionCandidate candidate
		WHERE candidate.id = :candidateId
		""")
	Optional<MentionCandidate> findByIdForUpdate(
			@Param("candidateId") UUID candidateId
	);
	
	@Query("""
		SELECT candidate.diaryId
		FROM MentionCandidate candidate
		WHERE candidate.id = :candidateId
		""")
	Optional<UUID> findDiaryIdById(
			@Param("candidateId") UUID candidateId
	);
}