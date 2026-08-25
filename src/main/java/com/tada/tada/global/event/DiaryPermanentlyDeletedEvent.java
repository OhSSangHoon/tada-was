package com.tada.tada.global.event;

import java.util.UUID;

/**
 * [발행: 민혁] — 30일 경과 후 pg_cron 자동 영구삭제, 또는 사용자의 명시적 영구삭제 시 발행
 * [구독: 한영] — DIARY_PERSON, MENTION_CANDIDATE row 완전 삭제 (이때는 mention_count 추가 조정 없음,
 *                    이미 TRASHED 시점에 감소 처리가 끝나 있으므로)
 * [구독: 형호] — 별도 처리 불필요 (diaries row 자체가 삭제되며 embedding도 같이 사라짐)
 */
public record DiaryPermanentlyDeletedEvent(UUID diaryId, UUID userId) {}