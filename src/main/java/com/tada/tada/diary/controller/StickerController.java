package com.tada.tada.diary.controller;

import com.tada.tada.diary.dto.StickerResponse;
import com.tada.tada.diary.dto.StickerSortOption;
import com.tada.tada.diary.service.StickerService;
import com.tada.tada.global.exception.CustomException;
import com.tada.tada.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/*
	StickerController - 스티커 앨범 조회 API
	
	GET /api/stickers?sort=latest&page=0&size=12	(기본값 - 최신순 0페이지 12개씩 조회)
	GET /api/stickers?sort=oldest&page=0&size=12	(오래된순)
	
	- 검색 API(SearchController)와 동일하게 Spring Page 구조 반환
	- Authentication에서 userId를 꺼내 본인이 모은 스티커만 조회
 */

@RestController
@RequiredArgsConstructor
public class StickerController {
	
	private static final String DEFAULT_SORT = "latest";
	private static final String DEFAULT_PAGE = "0";
	private static final String DEFAULT_PAGE_SIZE = "12";
	
	private final StickerService stickerService;
	
	@GetMapping("/api/stickers")
	public ApiResponse<Page<StickerResponse>> getMyStickers(
			@RequestParam(value = "sort", required = false, defaultValue = DEFAULT_SORT) String sort,
			@RequestParam(value = "page", required = false, defaultValue = DEFAULT_PAGE) int page,
			@RequestParam(value = "size", required = false, defaultValue = DEFAULT_PAGE_SIZE) int size,
			Authentication authentication
	) {
		UUID userId = (UUID) authentication.getPrincipal();
		
		// sort 문자열 검증 + enum 변환은 공유 StickerSortOption이 담당
		StickerSortOption sortOption = StickerSortOption.from(sort);
		
		if (page < 0) {
			throw new CustomException("page는 0 이상이어야 합니다.", 400);
		}
		if (size < 1) {
			throw new CustomException("size는 1 이상이어야 합니다.", 400);
		}
		
		Page<StickerResponse> result = stickerService.getAllStickers(userId, sortOption, page, size);
		
		return ApiResponse.success(result);
	}
}
