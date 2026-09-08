package com.tada.tada.sticker.service;

import com.tada.tada.diary.entity.Sticker;
import com.tada.tada.diary.repository.StickerRepository;
import com.tada.tada.sticker.dto.StickerAlbumResponse;
import com.tada.tada.sticker.dto.StickerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/*
	StickerService - 스티커 앨범 비즈니스 로직
	
	로그인한 사용자가 모은 스티커 전체 목록을 정렬 옵션에 따라 조회
 */
@Service
@RequiredArgsConstructor
public class StickerService {
	
	private  static final String SORT_OLDEST = "oldest";
	
	private  final StickerRepository stickerRepository;
	
	public StickerAlbumResponse getMyStickers(UUID userId, String sort) {
		
		Sort sortOrder = SORT_OLDEST.equals(sort)
				? Sort.by("createdAt").ascending()
				: Sort.by("createdAt").descending();
		
		List<Sticker> stickers = stickerRepository.findByUserId(userId, sortOrder);
		
		List<StickerResponse> stickerResponses = stickers.stream()
				.map(StickerResponse::from)
				.toList();
		
		return new StickerAlbumResponse(stickerResponses);
	}
}
