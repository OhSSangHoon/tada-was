package com.tada.tada.global.event;

import java.util.UUID;

/**
 * [발행: 민혁] — 일기가 휴지통으로 이동됐을 때(소프트삭제) 발행
 * [구독: 한영] — 관련 인물들의 PERSON_AGGREGATE.mention_count 즉시 감소
 *                   (DIARY_PERSON 연결 자체는 안 지움 — 복구 대비)
 */
public record DiaryTrashedEvent(UUID diaryId, UUID userId) {}