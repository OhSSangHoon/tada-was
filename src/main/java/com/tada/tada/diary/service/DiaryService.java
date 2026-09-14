package com.tada.tada.diary.service;

import com.tada.tada.diary.dto.CanCreateResponse;
import com.tada.tada.diary.dto.DiaryCreateForm;
import com.tada.tada.diary.dto.DiaryResponse;
import com.tada.tada.diary.dto.DiaryUpdateForm;
import com.tada.tada.diary.entity.Diary;
import com.tada.tada.diary.entity.DiaryStatus;
import com.tada.tada.diary.entity.Sticker;
import com.tada.tada.diary.repository.DiaryRepository;
import com.tada.tada.diary.repository.StickerRepository;
import com.tada.tada.global.event.DiaryCreatedEvent;
import com.tada.tada.global.event.DiaryRestoredEvent;
import com.tada.tada.global.event.DiaryTrashedEvent;
import com.tada.tada.global.event.MentionExtractedEvent;
import com.tada.tada.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

	private final DiaryRepository diaryRepository;
	private final StickerRepository stickerRepository;
	private final ApplicationEventPublisher eventPublisher;
	private static final int NEARBY_DATE_RANGE_DAYS = 3;
	private static final int DAILY_CREATE_LIMIT = 5;
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	
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
		
		Sticker sticker = Sticker.builder()
				.diaryId(savedDiary.getId())
				.imageUrl(form.getImageUrl())
				.keyword(form.getKeyword())
				.type(form.getType())
				.build();
				
		stickerRepository.save(sticker);
		
		eventPublisher.publishEvent(
				new MentionExtractedEvent(savedDiary.getId(), userId, form.getExtractionResult())
		);
		eventPublisher.publishEvent(
				new DiaryCreatedEvent(savedDiary.getId(), userId)
		);
		
		return DiaryResponse.from(savedDiary);
	}
	
	public DiaryResponse getDiary(UUID userId, UUID diaryId) {
		Diary diary = diaryRepository.findById(diaryId)
				.orElseThrow(() -> new CustomException("일기를 찾을 수 없습니다.", 404));
		
		if (!diary.getUserId().equals(userId)) {
			throw new CustomException("접근 권한이 없습니다.", 403);
		}
		
		if (!diary.isActive()) {
			throw new CustomException("일기를 찾을 수 없습니다.", 404);
		}
		
		return DiaryResponse.from(diary);
	}
	
	@Transactional
	public DiaryResponse updateDiary(UUID userId, UUID diaryId, DiaryUpdateForm form) {
		Diary diary = diaryRepository.findById(diaryId)
				.orElseThrow(() -> new CustomException("일기를 찾을 수 없습니다.", 404));
		
		if (!diary.getUserId().equals(userId)) {
			throw new CustomException("접근 권한이 없습니다.", 403);
		}
		
		if (!diary.isActive()) {
			throw new CustomException("일기를 찾을 수 없습니다.", 404);
		}
		
		diary.update(form.getTitle(), form.getWeather(), form.getContent());
		
		return DiaryResponse.from(diary);
	}
	
	@Transactional
	public void trashDiary(UUID userId, UUID diaryId) {
		Diary diary = diaryRepository.findById(diaryId)
				.orElseThrow(() -> new CustomException("일기를 찾을 수 없습니다.", 404));
		
		if (!diary.getUserId().equals(userId)) {
			throw new CustomException("접근 권한이 없습니다.", 403);
		}
		
		if (!diary.isActive()) {
			throw new CustomException("일기를 찾을 수 없습니다.", 404);
		}
		
		diary.trash();
		
		eventPublisher.publishEvent(new DiaryTrashedEvent(diaryId, userId));
	}
	
	public List<DiaryResponse> getAllTrashedDiaries(UUID userId) {
		List<Diary> diaries = diaryRepository.findByUserIdAndStatus(userId, DiaryStatus.TRASHED);
		return diaries.stream().map(DiaryResponse::from).toList();
	}
	
	@Transactional
	public DiaryResponse restoreDiary(UUID userId, UUID diaryId, boolean replace) {
		Diary target = diaryRepository.findByIdForUpdate(diaryId)
				.orElseThrow(() -> new CustomException("일기를 찾을 수 없습니다.", 404));
		
		if (!target.getUserId().equals(userId)) {
			throw new CustomException("접근 권한이 없습니다.", 403);
		}
		
		if (target.isActive()) {
			throw new CustomException("일기를 찾을 수 없습니다.", 404);
		}
		
		Optional<Diary> existingActive = diaryRepository.findByUserIdAndEntryDateAndStatusForUpdate(
				userId, target.getEntryDate(), DiaryStatus.ACTIVE);
		
		if (existingActive.isPresent()) {
			if (!replace) {
				throw new CustomException("같은 날짜에 이미 일기가 있습니다.", 409);
			}
			Diary existing = existingActive.get();
			existing.trash();
			eventPublisher.publishEvent(new DiaryTrashedEvent(existing.getId(), userId));
		}
		
		target.restore();
		eventPublisher.publishEvent(new DiaryRestoredEvent(target.getId(), userId));
		
		return DiaryResponse.from(target);
	}
	
	public CanCreateResponse canCreate(UUID userId, LocalDate date) {
		
		Optional<Diary> existing = diaryRepository.findByUserIdAndEntryDateAndStatus(userId, date, DiaryStatus.ACTIVE);
		if (existing.isPresent()){
			return CanCreateResponse.builder()
					.canCreate(false)
					.reason("해당 날짜에 이미 일기가 있습니다.")
					.build();
		}
		
		LocalDateTime todayStart = LocalDate.now(KST).atStartOfDay();
		long todayCount = diaryRepository.countByUserIdAndCreatedAtAfter(userId, todayStart);
		if (todayCount >= DAILY_CREATE_LIMIT) {
			return CanCreateResponse.builder()
					.canCreate(false)
					.reason("하루 생성 횟수 5회 초과")
					.build();
		}
		
		return CanCreateResponse.builder()
				.canCreate(true)
				.reason(null)
				.build();
	}
	
	public List<DiaryResponse> getNearbyDiaries(UUID userId, LocalDate targetDate) {
		LocalDate start = targetDate.minusDays(NEARBY_DATE_RANGE_DAYS);
		LocalDate end = targetDate.plusDays(NEARBY_DATE_RANGE_DAYS);
		
		List<Diary> diaries = diaryRepository.findByUserIdAndEntryDateBetweenAndStatus(userId, start, end, DiaryStatus.ACTIVE);
		return diaries.stream().map(DiaryResponse::from).toList();
	}
}
