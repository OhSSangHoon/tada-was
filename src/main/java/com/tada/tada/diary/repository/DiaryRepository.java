package com.tada.tada.diary.repository;

import com.tada.tada.diary.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface DiaryRepository extends JpaRepository<Diary, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT diary FROM Diary diary WHERE diary.id = :diaryId")
	Optional<Diary> findByIdForUpdate(
			@Param("diaryId") UUID diaryId
	);
}
