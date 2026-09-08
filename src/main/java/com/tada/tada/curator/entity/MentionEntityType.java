package com.tada.tada.curator.entity;

/*
 * mention_candidate.entity_type 의 허용 값.
 *
 * 실제 컬럼은 varchar 이므로 @Enumerated(EnumType.STRING) 으로 매핑한다.
 * DB ENUM 타입은 값 추가 때 스키마 변경이 필요해 사용하지 않는다. (ERD 공통 규칙)
 *
 * AI 계약 DTO 의 entityType 은 Gemini 응답 원문이므로 String 으로 남기고,
 * 저장 계층과 서비스 내부에서만 이 enum 을 사용한다.
 */
public enum MentionEntityType {
	PERSON,
	PLACE,
	ACTIVITY;

	public boolean isSource() {
		return this == PLACE
				|| this == ACTIVITY;
	}
}
