package com.tada.tada.global.event;

import java.util.UUID;

/**
 * [발행: 민혁] — 일기 본문이 실제로 수정 저장됐을 때 발행 (제목/날씨만 바뀐 경우는 발행 안 함)
 * [구독: 형호] — 재임베딩 (Voyage AI 다시 호출해서 embedding 갱신)
 * 한영(Curator)은 이 이벤트를 구독하지 않음 — 본문 수정 시에도 MentionExtractedEvent로 처리함 (2026-09-08 한영 확인)
 *
 * oldContent, newContent: diff 비교를 위해 수정 전/후 본문을 같이 전달
 */
public record DiaryUpdatedEvent(UUID diaryId, UUID userId, String oldContent, String newContent) {}