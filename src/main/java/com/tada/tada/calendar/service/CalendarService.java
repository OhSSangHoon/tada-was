package com.tada.tada.calendar.service;

import com.tada.tada.calendar.dto.CalendarResponse;
import com.tada.tada.diary.entity.Diary;
import com.tada.tada.diary.entity.DiaryStatus;
import com.tada.tada.diary.entity.Sticker;
import com.tada.tada.diary.repository.DiaryRepository;
import com.tada.tada.diary.repository.StickerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

	private final DiaryRepository diaryRepository;
	private final StickerRepository stickerRepository;
	
	public List<CalendarResponse> getCalendar(UUID userId, int year, int month) {
		
		LocalDate start = LocalDate.of(year, month, 1);
		LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
		
		List<Diary> diaries = diaryRepository.findByUserIdAndEntryDateBetweenAndStatus(userId, start, end, DiaryStatus.ACTIVE);
		
		List<UUID> diaryIds = diaries.stream().map(Diary::getId).toList();
		List<Sticker> stickers = stickerRepository.findByDiaryIdIn(diaryIds);
		
		Map<UUID,Sticker> stickerMap = stickers.stream()
				.collect(Collectors.toMap(Sticker::getDiaryId, sticker -> sticker));
		
		return diaries.stream()
				.filter(diary -> stickerMap.containsKey(diary.getId()))
				.map(diary -> {
					Sticker sticker = stickerMap.get(diary.getId());
					return CalendarResponse.builder()
							.diaryId(diary.getId())
							.entryDate(diary.getEntryDate())
							.imageUrl(sticker.getImageUrl())
							.keyword(sticker.getKeyword())
							.type(sticker.getType())
							.build();
				})
				.toList();
	}
}
