package com.tada.tada.diary.repository;

import com.tada.tada.diary.entity.Diary;
import com.tada.tada.diary.entity.DiaryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiaryRepository extends JpaRepository<Diary, UUID> {

	List<Diary> findByUserIdAndEntryDateBetweenAndStatus(UUID userId, LocalDate start, LocalDate end, DiaryStatus status);
	
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT diary FROM Diary diary WHERE diary.id = :diaryId")
	Optional<Diary> findByIdForUpdate(
			@Param("diaryId") UUID diaryId
	);
}
