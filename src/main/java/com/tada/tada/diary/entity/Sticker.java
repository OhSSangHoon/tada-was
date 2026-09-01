package com.tada.tada.diary.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stickers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sticker {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "diary_id", nullable = false, unique = true)
    private UUID diaryId;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private String keyword;

    // 추출형 / 압축형
    @Column(nullable = false)
    private String type;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Sticker(UUID diaryId, String imageUrl, String keyword, String type) {
        this.diaryId = diaryId;
        this.imageUrl = imageUrl;
        this.keyword = keyword;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }
}
