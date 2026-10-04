package com.tada.tada.search.dto;

/*
    SearchSortOption - 검색 결과 정렬 옵션
    
    - StickerSortOption과 동일한 패턴
    - 정렬 기준(관련도순/날짜순)에 따라 SearchRepository의 서로 다른 쿼리 메서드를 호출하는 데 사용
 */

import com.tada.tada.global.exception.CustomException;
import org.springframework.data.domain.Sort;

public enum SearchSortOption {
	
	LATEST(Sort.Direction.DESC),
	OLDEST(Sort.Direction.ASC),
	RELEVANCE(Sort.Direction.ASC);   // 코사인 거리 오름차순(가까운 일기가 먼저)
	
	private final Sort.Direction direction;
	
	SearchSortOption(Sort.Direction direction) {
		this.direction = direction;
	}
	
	public Sort.Direction getDirection() {
		return direction;
	}
	
	public static SearchSortOption from(String value) {
		try {
			return SearchSortOption.valueOf(value.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new CustomException("Sort는 latest, oldest, relevance만 가능합니다", 400);
		}
	}
}
