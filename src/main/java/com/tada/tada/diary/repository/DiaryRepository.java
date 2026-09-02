package com.tada.tada.diary.repository;

import com.tada.tada.diary.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DiaryRepository extends JpaRepository<Diary, UUID> {
}
