package com.tada.tada.diary.dto;

import com.tada.tada.diary.entity.Diary;
import com.tada.tada.diary.entity.Sticker;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class TrashedDiaryResponse {
	private UUID id;
	private LocalDate entryDate;
	private String title;
	private String weather;
	private String content;
	private String imageUrl;
	private String keyword;
	
	public static TrashedDiaryResponse of(Diary diary, Sticker sticker) {
		return TrashedDiaryResponse.builder()
				.id(diary.getId())
				.entryDate(diary.getEntryDate())
				.title(diary.getTitle())
				.weather(diary.getWeather())
				.content(diary.getContent())
				.imageUrl(sticker == null ? null : sticker.getImageUrl())
				.keyword(sticker == null ? null : sticker.getKeyword())
				.build();
	}
}
