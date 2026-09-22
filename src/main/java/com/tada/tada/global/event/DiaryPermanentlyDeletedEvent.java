package com.tada.tada.global.event;

import java.util.UUID;

/**
 * 사용되지 않음 (2026-09-06 확정) — 30일 경과 영구삭제는 pg_cron이 아니라 Spring @Scheduled 배치로 구현되고,
 * 이벤트 발행 대신 민혁의 영구삭제 오케스트레이션 메서드가 한영의 CuratorCleanupService.deleteByDiaryId()를
 * 같은 트랜잭션에서 절차적으로 직접 호출하는 방식으로 대체됨. 이 클래스는 참고용으로만 남겨둠.
 */
public record DiaryPermanentlyDeletedEvent(UUID diaryId, UUID userId) {}