package com.tada.tada.diary.dto;

import com.tada.tada.diary.entity.Diary;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class DiaryResponse {
	private UUID id;
	private LocalDate entryDate;
	private String title;
	private String weather;
	private String content;
	
	public static DiaryResponse from(Diary diary) {
		return DiaryResponse.builder()
				.id(diary.getId())
				.entryDate(diary.getEntryDate())
				.title(diary.getTitle())
				.weather(diary.getWeather())
				.content(diary.getContent())
				.build();
	}
}
