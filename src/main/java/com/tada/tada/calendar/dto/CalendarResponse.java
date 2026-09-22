package com.tada.tada.calendar.dto;

import com.tada.tada.diary.entity.StickerType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class CalendarResponse {
	private UUID diaryId;
	private LocalDate entryDate;
	private String imageUrl;
	private String keyword;
	private StickerType type;
}
