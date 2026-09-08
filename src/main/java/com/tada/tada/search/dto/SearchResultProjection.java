package com.tada.tada.search.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/*
	검색 결과 조회용 프로젝션 인터페이스
	
	Diary 엔티티 대신 이 인터페이스로 쿼리 결과를 직접 매핑 받는다.
	
	네이티브 쿼리에서 각 컬럼에 AS id, AS entryDate처럼 별칭을 붙여야 아래 Getter 이름과 정확히 매칭
 */
public interface SearchResultProjection {
	UUID getId();
	LocalDate getEntryDate();
	String getTitle();
	String getWeather();
	String getContent();
	LocalDateTime getCreatedAt();
}
