package com.tada.tada.global.event;

import java.util.UUID;

/**
 * [발행: 민혁] — 일기 작성 완료 후 저장 직후 발행
 * [구독: 형호] — 이 이벤트 받아서 Voyage AI로 임베딩 생성 후 diaries.embedding에 저장
 */
public record DiaryCreatedEvent(UUID diaryId, UUID userId) {}