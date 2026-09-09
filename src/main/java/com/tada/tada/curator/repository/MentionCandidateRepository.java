package com.tada.tada.curator.repository;

import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.entity.MentionCandidateStatus;
import com.tada.tada.curator.entity.MentionEntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

	/*
	 * 그 사람과 함께한 장소·활동 집계.
	 *
	 * CONFIRMED PERSON Candidate -> mention_candidate_person_ref -> source Candidate
	 * 경로로만 간다. `그 사람이 나온 일기의 모든 PLACE/ACTIVITY` 를 세면
	 * 혼자 한 식사나 산책까지 함께한 것으로 잡힌다.
	 *
	 * COUNT(DISTINCT source.diaryId) 로 센다. 한 일기에 같은 표현이 여러 번
	 * 나와도 1회이고, 한 source 가 같은 사람의 PERSON Candidate 두 개에
	 * 연결돼 행이 늘어나도 결과가 부풀지 않는다.
	 *
	 * 정렬을 normalizedText 까지 끊는다. 같은 일기에 장소가 두 곳 나오면
	 * diaryCount 와 lastEntryDate 가 둘 다 같아져 순서가 비결정적이 된다.
	 *
	 * Top3, 타임라인 카드 키워드, 추억 그룹이 모두 이 결과를 쓴다.
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
}