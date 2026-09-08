package com.tada.tada.curator.repository;

import com.tada.tada.curator.entity.PersonAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PersonAggregateRepository
		extends JpaRepository<PersonAggregate, UUID> {

	@Query("""
			SELECT
				diaryPerson.personId AS personId,
				COUNT(DISTINCT diary.id) AS mentionCount,
				MAX(diary.entryDate) AS lastMentionedDate
			FROM DiaryPerson diaryPerson, Diary diary
			WHERE diaryPerson.diaryId = diary.id
			  AND diary.userId = :userId
			  AND diary.status = com.tada.tada.diary.entity.DiaryStatus.ACTIVE
			  AND diaryPerson.personId IN :personIds
			GROUP BY diaryPerson.personId
			""")
	List<PersonAggregateSnapshot> findActiveSnapshots(
			@Param("userId") UUID userId,
			@Param("personIds") Collection<UUID> personIds
	);

	interface PersonAggregateSnapshot {

		UUID getPersonId();

		long getMentionCount();

		LocalDate getLastMentionedDate();
	}
}