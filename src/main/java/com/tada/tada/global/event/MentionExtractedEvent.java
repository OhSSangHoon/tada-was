package com.tada.tada.global.event;

import java.util.UUID;
import java.util.List;

/**
 * [발행: 민혁] — n8n 콜백으로 인물·장소 원문 후보가 도착했을 때 발행
 * [구독: 한영] — 이 이벤트 받아서 정규화 매칭 4단계 로직 실행
 *
 * mentionCandidateIds: 이미 DB에 저장된 MentionCandidate row들의 id 목록
 *                       (원문 텍스트 자체는 여기 안 담고, id로 넘겨서 E가 직접 조회하게 함)
 */
public record MentionExtractedEvent(UUID diaryId, List<UUID> mentionCandidateIds) {}