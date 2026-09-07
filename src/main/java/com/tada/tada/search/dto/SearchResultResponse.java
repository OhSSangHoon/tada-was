package com.tada.tada.search.dto;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/*
	SearchResultResponse - 검색 결과 응답 DTO
	
	Response DTO 컨벤션
	- @Getter만 사용 (불변이라 Setter 필요없음)
	- "Response" 접미사
	- Entity(diary)를 그대로 반환하지 않고 필요한 필드만 골라서 노출
		(embedding 같은 경우 제외)
 */
@Getter
public class SearchResultResponse {
	
	private final UUID id;
	private final LocalDate entryDate;
	private final String title;
	private final String weather;
	private final String content;
	private final LocalDateTime createdAt;
	
	public SearchResultResponse(UUID id, LocalDate entryDate, String title,
								String weather, String content, LocalDateTime createdAt) {
		this.id = id;
		this.entryDate = entryDate;
		this.title = title;
		this.weather = weather;
		this.content = content;
		this.createdAt = createdAt;
	}
}
