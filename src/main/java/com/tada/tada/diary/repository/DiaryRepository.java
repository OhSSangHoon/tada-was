package com.tada.tada.diary.repository;

import com.tada.tada.diary.entity.Diary;
import com.tada.tada.diary.entity.DiaryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DiaryRepository extends JpaRepository<Diary, UUID> {

	List<Diary> findByUserIdAndEntryDateBetweenAndStatus(UUID userId, LocalDate start, LocalDate end, DiaryStatus status);
}
