package com.tada.tada.diary.dto;

import lombok.Getter;

/*
	GenerateStickerResponse - 스티커 생성/재생성 트리거 응답 DTO

	DB 저장 없음(CLAUDE.md 스펙) - n8n이 만든 이미지를 Supabase Storage에 올린
	영구 URL만 돌려준다. 사용자가 최종 확인해야 POST /api/diaries로 진짜 저장된다.
 */
@Getter
public class GenerateStickerResponse {

	private final String imageUrl;

	public GenerateStickerResponse(String imageUrl) {
		this.imageUrl = imageUrl;
	}
}
