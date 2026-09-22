package com.tada.tada.global.event;

import java.util.UUID;

/**
 * [발행: 민혁] — 휴지통에서 복원됐을 때 발행
 * [구독: 한영] — 관련 인물들의 PERSON_AGGREGATE.mention_count를 ACTIVE 일기 기준으로 재계산
 *                   (증감이 아니라 원본 재계산이라 중복/순서가 꼬여도 최종값은 항상 동일)
 */
public record DiaryRestoredEvent(UUID diaryId, UUID userId) {}