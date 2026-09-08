package com.tada.tada.global.event;

import java.util.UUID;

/**
 * [발행: 민혁] — 휴지통에서 복원됐을 때 발행 (같은 날짜 ACTIVE 있어서 교체하는 경우엔 DiaryTrashedEvent와 함께 발행됨)
 * [구독: 한영] — mention_count를 증감시키지 않고, ACTIVE 일기 원본에서 매번 다시 계산하는 방식으로 대체됨
 */
public record DiaryRestoredEvent(UUID diaryId, UUID userId) {}