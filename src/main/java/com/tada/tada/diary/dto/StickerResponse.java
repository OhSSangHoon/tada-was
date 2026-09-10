package com.tada.tada.diary.dto;

import com.tada.tada.diary.entity.Sticker;
import com.tada.tada.diary.entity.StickerType;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/*
	StickerResponse - 스티커 한 개에 대한 응답 DTO
	
	Response DTO 컨벤션
	- @Getter만 사용, "Response" 접미사
	- Entity(Sticker)를 그대로 반환하지 않고 필요한 필드만 노출
 */
@Getter
public class StickerResponse {
	
	private final UUID id;
	private final UUID diaryId;
	private final String imageUrl;
	private final String keyword;
	private final StickerType type;
	private final LocalDateTime createdAt;
	
	public StickerResponse(UUID id, UUID diaryId, String imageUrl, String keyword,
						   StickerType type, LocalDateTime createdAt) {
		this.id = id;
		this.diaryId = diaryId;
		this.imageUrl = imageUrl;
		this.keyword = keyword;
		this.type = type;
		this.createdAt = createdAt;
	}
	
	public static StickerResponse from(Sticker sticker) {
		return new StickerResponse(
				sticker.getId(), sticker.getDiaryId(), sticker.getImageUrl(),
				sticker.getKeyword(), sticker.getType(), sticker.getCreatedAt()
		);
	}
}
