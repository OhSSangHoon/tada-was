package com.tada.tada.diary.repository;

import com.tada.tada.diary.entity.Sticker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StickerRepository extends JpaRepository<Sticker, UUID> {
}
