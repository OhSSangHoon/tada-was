package com.tada.tada.diary.repository;

import com.tada.tada.diary.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DiaryRepository extends JpaRepository<Diary, UUID> {

	List<Diary> findByUserIdAndEntryDateBetween(UUID userId, LocalDate start, LocalDate end);
}
