package com.tada.tada.global.event;

import java.util.UUID;

/**
 * [발행: 민혁] — 일기 본문이 실제로 수정 저장됐을 때 발행 (제목/날씨만 바뀐 경우는 발행 안 함)
 * [구독: 형호] — 재임베딩 (Voyage AI 다시 호출해서 embedding 갱신)
 * [구독: 한영] — 원문 diff 기반 재매칭 (MentionExtractedEvent와 달리, 기존 연결과 비교해서 변경분만 처리)
 *
 * oldContent, newContent: diff 비교를 위해 수정 전/후 본문을 같이 전달
 */
public record DiaryUpdatedEvent(UUID diaryId, UUID userId, String oldContent, String newContent) {}