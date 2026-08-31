package com.tada.tada.diary.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "diaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Diary {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    // 20자 제한은 애플리케이션(Service) 레벨에서 검증
    @Column(nullable = false)
    private String title;

    // nullable
    private String weather;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    // TODO: pgvector 컬럼(embedding) — RAG 담당(형호)이 임베딩 저장 시 매핑 방식 결정 예정.
    // 지금은 ddl-auto: none이라 엔티티에서 매핑 안 해도 스키마 검증에는 영향 없음.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiaryStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Diary(UUID userId, LocalDate entryDate, String title, String weather, String content) {
        this.userId = userId;
        this.entryDate = entryDate;
        this.title = title;
        this.weather = weather;
        this.content = content;
        this.status = DiaryStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public void update(String title, String content, String weather) {
        this.title = title;
        this.content = content;
        this.weather = weather;
    }

    public void trash() {
        this.status = DiaryStatus.TRASHED;
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.status = DiaryStatus.ACTIVE;
        this.deletedAt = null;
    }

    public boolean isActive() {
        return this.status == DiaryStatus.ACTIVE;
    }
}
