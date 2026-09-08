package com.tada.tada.global.event;

import java.util.UUID;

/**
 * [발행: 민혁] — 일기가 휴지통으로 이동됐을 때(소프트삭제) 발행
 * [구독: 한영] — mention_count를 증감시키지 않고, ACTIVE 일기 원본에서 매번 다시 계산하는 방식으로 대체됨
 *                   (DIARY_PERSON 연결 자체는 안 지움 — 복구 대비)
 */
public record DiaryTrashedEvent(UUID diaryId, UUID userId) {}