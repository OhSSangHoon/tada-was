package com.tada.tada.curator.entity;

/*
 * varchar 컬럼을 @Enumerated(STRING)으로 매핑한다 (DB ENUM은 값 추가 시 스키마 변경 필요, ERD 공통 규칙).
 * AI 계약 DTO의 entityType은 String 그대로 두고, 내부에서만 이 enum을 쓴다.
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
