package com.tada.tada.diary.service;

import com.tada.tada.diary.dto.DiaryCreateForm;
import com.tada.tada.diary.dto.DiaryResponse;
import com.tada.tada.diary.entity.Diary;
import com.tada.tada.diary.repository.DiaryRepository;
import com.tada.tada.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

	private final DiaryRepository diaryRepository;
	
	@Transactional
	public DiaryResponse createDiary(UUID userId, DiaryCreateForm form) {
		Diary diary = Diary.builder()
				.userId(userId)
				.entryDate(form.getEntryDate())
				.title(form.getTitle())
				.weather(form.getWeather())
				.content(form.getContent())
				.build();
		
		Diary savedDiary = diaryRepository.save(diary);
		return DiaryResponse.from(savedDiary);
	}
	
	public DiaryResponse getDiary(UUID userId, UUID diaryId) {
		Diary diary = diaryRepository.findById(diaryId)
				.orElseThrow(() -> new CustomException("일기를 찾을 수 없습니다.", 404));
		
		if (!diary.getUserId().equals(userId)) {
			throw new CustomException("접근 권한이 없습니다.", 403);
		}
		
		return DiaryResponse.from(diary);
	}
}
