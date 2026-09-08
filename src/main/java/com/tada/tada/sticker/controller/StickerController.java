package com.tada.tada.sticker.controller;


import com.tada.tada.global.exception.CustomException;
import com.tada.tada.global.response.ApiResponse;
import com.tada.tada.sticker.dto.StickerAlbumResponse;
import com.tada.tada.sticker.service.StickerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/*
	StickerController - 스티커 앨범 조회 API 엔드포인트
	
	GET /api/stickers?sort=latest	(기본값, 최신순)
	GET /api/stickers?sort=oldest	(오래된순)
	
	- 패이지네이션 없이 사용자가 모은 스티커 전체를 한 번에 반환 (프론트 무한 스크롤)
	- Authentication에서 userId를 꺼내 본인이 모은 스티커만 조회
 */
@RestController
@RequiredArgsConstructor
public class StickerController {
	
	private static final String DEFAULT_SORT = "latest";
	private static final String SORT_LATEST = "latest";
	private static final String SORT_OLDEST = "oldest";
	
	private final StickerService stickerService;
	
	@GetMapping("/api/stickers")
	public ApiResponse<StickerAlbumResponse> getMyStickers(
			@RequestParam(value = "sort", required = false, defaultValue = DEFAULT_SORT) String sort,
			Authentication authentication
	) {
		UUID userId = (UUID) authentication.getPrincipal();
		
		if (!SORT_LATEST.equals(sort) && !SORT_OLDEST.equals(sort)) {
			throw new CustomException("sort는 latest 또는 oldest만 가능합니다.", 400);
		}
		
		StickerAlbumResponse result = stickerService.getMyStickers(userId, sort);
		
		return ApiResponse.success(result);
	}
}
