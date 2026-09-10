package com.tada.tada.diary.service;

import com.tada.tada.diary.dto.StickerResponse;
import com.tada.tada.diary.dto.StickerSortOption;
import com.tada.tada.diary.entity.DiaryStatus;
import com.tada.tada.diary.entity.Sticker;
import com.tada.tada.diary.repository.StickerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

/*
	StickerService - 스티커 앨범 비즈니스 로직
	
	로그인한 사용자가 모은 스티커를 정렬 옵션 + 페이지네이션으로 조회
 */
@Service
@RequiredArgsConstructor
public class StickerService {
	
	private final StickerRepository stickerRepository;
	
	public Page<StickerResponse> getAllStickers(UUID userId, StickerSortOption sortOption, int page, int size) {
		
		Sort sort = Sort.by(sortOption.getDirection(), "createdAt");
		Pageable pageable = PageRequest.of(page, size, sort);
		
		Page<Sticker> stickers = stickerRepository.findByUserId(userId, DiaryStatus.ACTIVE, pageable);
		
		return stickers.map(StickerResponse::from);
	}
}
