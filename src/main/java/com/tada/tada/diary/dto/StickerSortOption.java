package com.tada.tada.diary.dto;

import com.tada.tada.global.exception.CustomException;
import org.springframework.data.domain.Sort;

/*
	StickerSortOption - 스티커 앨범 정렬 옵션
	
	- Controller/Service에 각각 "latest"/"oldest" 문자열 상수가 따로 있으면
		한쪽만 수정될 때 컴파일 에러 없이 런타임 버그가 날 수 있어서 한곳으로 통합
	- 클라이언트 입력(query string)을 여기서 검증 + Sort.Direction으로 변환
 */
public enum StickerSortOption {
	
	LATEST(Sort.Direction.DESC),
	OLDEST(Sort.Direction.ASC);
	
	private final Sort.Direction direction;
	
	StickerSortOption(Sort.Direction direction) {
		this.direction = direction;
	}
	
	public Sort.Direction getDirection() {
		return direction;
	}
	
	// 쿼리 파라미터 문자열("latest"/"oldest")을 검증하면서 enum으로 변환
	public static StickerSortOption from(String value) {
		try {
			return StickerSortOption.valueOf(value.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new CustomException("sort는 latest 또는 oldest만 가능합니다.", 400);
		}
	}
}
