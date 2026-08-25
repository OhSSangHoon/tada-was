package com.tada.tada.global.event;

import java.util.UUID;

/**
 * [발행: 민혁] — 휴지통에서 복원됐을 때 발행
 * [구독: 한영] — 감소시켰던 mention_count 다시 증가 (재포함)
 */
public record DiaryRestoredEvent(UUID diaryId, UUID userId) {}