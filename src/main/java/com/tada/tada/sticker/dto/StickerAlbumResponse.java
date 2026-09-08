package com.tada.tada.sticker.dto;

import lombok.Getter;

import java.util.List;

/*
	StickerAlbumResponse - 스티커 앨범 전체 조회 응답 DTO
	
	- totalCount: 앨범 상단에 스티커 갯수 표시
	- stickers: 정렬된(최신순/오래된순) 스티커 전체 리스트 (페이지네이션 없음)
 */
@Getter
public class StickerAlbumResponse {
	
	private final int totalCount;
	private final List<StickerResponse> stickers;
	
	public StickerAlbumResponse(List<StickerResponse> stickers) {
		this.stickers = stickers;
		this.totalCount = stickers.size();
	}
}
