package com.tada.tada.diary.service;

import com.tada.tada.diary.dto.CanCreateResponse;
import com.tada.tada.diary.dto.DiaryResponse;
import com.tada.tada.diary.dto.DiaryUpdateForm;
import com.tada.tada.diary.entity.Diary;
import com.tada.tada.diary.entity.DiaryStatus;
import com.tada.tada.diary.repository.DiaryRepository;
import com.tada.tada.diary.repository.StickerRepository;
import com.tada.tada.curator.service.CuratorCleanupService;
import com.tada.tada.global.event.DiaryRestoredEvent;
import com.tada.tada.global.event.DiaryTrashedEvent;
import com.tada.tada.global.event.DiaryUpdatedEvent;
import com.tada.tada.global.event.MentionExtractedEvent;
import com.tada.tada.global.event.dto.ExtractionResult;
import com.tada.tada.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DiaryServiceTest {

	private DiaryRepository diaryRepository;
	private StickerRepository stickerRepository;
	private CuratorCleanupService curatorCleanupService;
	private ApplicationEventPublisher eventPublisher;
	private DiaryService diaryService;

	@BeforeEach
	void setUp() {
		diaryRepository =
				Mockito.mock(DiaryRepository.class);

		stickerRepository =
				Mockito.mock(StickerRepository.class);

		curatorCleanupService =
				Mockito.mock(CuratorCleanupService.class);

		eventPublisher =
				Mockito.mock(ApplicationEventPublisher.class);

		diaryService =
				new DiaryService(
						diaryRepository,
						stickerRepository,
						curatorCleanupService,
						eventPublisher
				);
	}

	// ----- updateDiary -----

	@Test
	void 수정_대상이_없으면_404를_던진다() {
		UUID userId = UUID.randomUUID();
		UUID diaryId = UUID.randomUUID();
		DiaryUpdateForm form = new DiaryUpdateForm();

		when(diaryRepository.findById(diaryId))
				.thenReturn(Optional.empty());

		CustomException exception = assertThrows(
				CustomException.class,
				() -> diaryService.updateDiary(userId, diaryId, form)
		);

		assertEquals(404, exception.getStatusCode());
	}

	@Test
	void 다른_유저의_일기를_수정하려하면_403을_던진다() {
		UUID userId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		UUID diaryId = UUID.randomUUID();
		Diary diary = Mockito.mock(Diary.class);
		DiaryUpdateForm form = new DiaryUpdateForm();

		when(diaryRepository.findById(diaryId))
				.thenReturn(Optional.of(diary));
		when(diary.getUserId()).thenReturn(ownerId);

		CustomException exception = assertThrows(
				CustomException.class,
				() -> diaryService.updateDiary(userId, diaryId, form)
		);

		assertEquals(403, exception.getStatusCode());
	}

	@Test
	void TRASHED_상태의_일기를_수정하려하면_404를_던진다() {
		UUID userId = UUID.randomUUID();
		UUID diaryId = UUID.randomUUID();
		Diary diary = Mockito.mock(Diary.class);
		DiaryUpdateForm form = new DiaryUpdateForm();

		when(diaryRepository.findById(diaryId))
				.thenReturn(Optional.of(diary));
		when(diary.getUserId()).thenReturn(userId);
		when(diary.isActive()).thenReturn(false);

		CustomException exception = assertThrows(
				CustomException.class,
				() -> diaryService.updateDiary(userId, diaryId, form)
		);

		assertEquals(404, exception.getStatusCode());
	}

	@Test
	void 제목이나_날씨만_바뀌면_이벤트를_발행하지_않는다() {
		UUID userId = UUID.randomUUID();
		UUID diaryId = UUID.randomUUID();
		Diary diary = Mockito.mock(Diary.class);
		DiaryUpdateForm form = new DiaryUpdateForm();
		form.setTitle("새 제목");
		form.setWeather("SUNNY");
		form.setContent("기존 본문");

		when(diaryRepository.findById(diaryId))
				.thenReturn(Optional.of(diary));
		when(diary.getUserId()).thenReturn(userId);
		when(diary.isActive()).thenReturn(true);
		when(diary.getContent()).thenReturn("기존 본문");

		diaryService.updateDiary(userId, diaryId, form);

		verify(diary).update("새 제목", "SUNNY", "기존 본문");
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void 본문이_바뀌는데_extractionResult가_없으면_400을_던지고_수정하지_않는다() {
		UUID userId = UUID.randomUUID();
		UUID diaryId = UUID.randomUUID();
		Diary diary = Mockito.mock(Diary.class);
		DiaryUpdateForm form = new DiaryUpdateForm();
		form.setTitle("제목");
		form.setContent("새로운 본문");

		when(diaryRepository.findById(diaryId))
				.thenReturn(Optional.of(diary));
		when(diary.getUserId()).thenReturn(userId);
		when(diary.isActive()).thenReturn(true);
		when(diary.getContent()).thenReturn("기존 본문");

		CustomException exception = assertThrows(
				CustomException.class,
				() -> diaryService.updateDiary(userId, diaryId, form)
		);

		assertEquals(400, exception.getStatusCode());
		verify(diary, never()).update(any(), any(), any());
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void 본문이_바뀌면_MentionExtractedEvent와_DiaryUpdatedEvent를_둘다_발행한다() {
		UUID userId = UUID.randomUUID();
		UUID diaryId = UUID.randomUUID();
		Diary diary = Mockito.mock(Diary.class);
		ExtractionResult extractionResult = new ExtractionResult(List.of(), List.of(), List.of());
		DiaryUpdateForm form = new DiaryUpdateForm();
		form.setTitle("제목");
		form.setContent("새로운 본문");
		form.setExtractionResult(extractionResult);

		when(diaryRepository.findById(diaryId))
				.thenReturn(Optional.of(diary));
		when(diary.getUserId()).thenReturn(userId);
		when(diary.isActive()).thenReturn(true);
		when(diary.getContent()).thenReturn("기존 본문");

		diaryService.updateDiary(userId, diaryId, form);

		verify(diary).update("제목", null, "새로운 본문");
		verify(eventPublisher).publishEvent(new MentionExtractedEvent(diaryId, userId, extractionResult));
		verify(eventPublisher).publishEvent(new DiaryUpdatedEvent(diaryId, userId, "기존 본문", "새로운 본문"));
	}

	// ----- restoreDiary -----

	@Test
	void 복원_대상이_없으면_404를_던진다() {
		UUID userId = UUID.randomUUID();
		UUID diaryId = UUID.randomUUID();

		when(diaryRepository.findByIdForUpdate(diaryId))
				.thenReturn(Optional.empty());

		CustomException exception = assertThrows(
				CustomException.class,
				() -> diaryService.restoreDiary(userId, diaryId, false)
		);

		assertEquals(404, exception.getStatusCode());
	}

	@Test
	void 다른_유저의_일기면_403을_던진다() {
		UUID userId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		UUID diaryId = UUID.randomUUID();
		Diary target = Mockito.mock(Diary.class);

		when(diaryRepository.findByIdForUpdate(diaryId))
				.thenReturn(Optional.of(target));
		when(target.getUserId()).thenReturn(ownerId);

		CustomException exception = assertThrows(
				CustomException.class,
				() -> diaryService.restoreDiary(userId, diaryId, false)
		);

		assertEquals(403, exception.getStatusCode());
	}

	@Test
	void 이미_ACTIVE인_일기는_404를_던진다() {
		UUID userId = UUID.randomUUID();
		UUID diaryId = UUID.randomUUID();
		Diary target = Mockito.mock(Diary.class);

		when(diaryRepository.findByIdForUpdate(diaryId))
				.thenReturn(Optional.of(target));
		when(target.getUserId()).thenReturn(userId);
		when(target.isActive()).thenReturn(true);

		CustomException exception = assertThrows(
				CustomException.class,
				() -> diaryService.restoreDiary(userId, diaryId, false)
		);

		assertEquals(404, exception.getStatusCode());
	}

	@Test
	void 같은_날짜에_ACTIVE가_있고_replace가_false면_409를_던지고_복원하지_않는다() {
		UUID userId = UUID.randomUUID();
		UUID diaryId = UUID.randomUUID();
		LocalDate entryDate = LocalDate.of(2026, 9, 10);
		Diary target = Mockito.mock(Diary.class);
		Diary existing = Mockito.mock(Diary.class);

		when(diaryRepository.findByIdForUpdate(diaryId))
				.thenReturn(Optional.of(target));
		when(target.getUserId()).thenReturn(userId);
		when(target.isActive()).thenReturn(false);
		when(target.getEntryDate()).thenReturn(entryDate);
		when(diaryRepository.findByUserIdAndEntryDateAndStatusForUpdate(
				userId, entryDate, DiaryStatus.ACTIVE))
				.thenReturn(Optional.of(existing));

		CustomException exception = assertThrows(
				CustomException.class,
				() -> diaryService.restoreDiary(userId, diaryId, false)
		);

		assertEquals(409, exception.getStatusCode());
		verify(target, never()).restore();
		verify(existing, never()).trash();
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void replace가_true면_기존_ACTIVE는_휴지통으로_대상은_복원된다() {
		UUID userId = UUID.randomUUID();
		UUID diaryId = UUID.randomUUID();
		UUID existingId = UUID.randomUUID();
		LocalDate entryDate = LocalDate.of(2026, 9, 10);
		Diary target = Mockito.mock(Diary.class);
		Diary existing = Mockito.mock(Diary.class);

		when(diaryRepository.findByIdForUpdate(diaryId))
				.thenReturn(Optional.of(target));
		when(target.getUserId()).thenReturn(userId);
		when(target.isActive()).thenReturn(false);
		when(target.getEntryDate()).thenReturn(entryDate);
		when(target.getId()).thenReturn(diaryId);
		when(diaryRepository.findByUserIdAndEntryDateAndStatusForUpdate(
				userId, entryDate, DiaryStatus.ACTIVE))
				.thenReturn(Optional.of(existing));
		when(existing.getId()).thenReturn(existingId);

		diaryService.restoreDiary(userId, diaryId, true);

		verify(existing).trash();
		verify(target).restore();
		verify(eventPublisher).publishEvent(new DiaryTrashedEvent(existingId, userId));
		verify(eventPublisher).publishEvent(new DiaryRestoredEvent(diaryId, userId));
	}

	@Test
	void 같은_날짜_ACTIVE가_없으면_바로_복원된다() {
		UUID userId = UUID.randomUUID();
		UUID diaryId = UUID.randomUUID();
		LocalDate entryDate = LocalDate.of(2026, 9, 10);
		Diary target = Mockito.mock(Diary.class);

		when(diaryRepository.findByIdForUpdate(diaryId))
				.thenReturn(Optional.of(target));
		when(target.getUserId()).thenReturn(userId);
		when(target.isActive()).thenReturn(false);
		when(target.getEntryDate()).thenReturn(entryDate);
		when(target.getId()).thenReturn(diaryId);
		when(diaryRepository.findByUserIdAndEntryDateAndStatusForUpdate(
				userId, entryDate, DiaryStatus.ACTIVE))
				.thenReturn(Optional.empty());

		diaryService.restoreDiary(userId, diaryId, false);

		verify(target).restore();
		verify(eventPublisher).publishEvent(new DiaryRestoredEvent(diaryId, userId));
	}

	// ----- canCreate -----

	@Test
	void 같은_날짜에_ACTIVE_일기가_있으면_생성불가를_반환한다() {
		UUID userId = UUID.randomUUID();
		LocalDate date = LocalDate.of(2026, 9, 10);
		Diary existing = Mockito.mock(Diary.class);

		when(diaryRepository.findByUserIdAndEntryDateAndStatus(userId, date, DiaryStatus.ACTIVE))
				.thenReturn(Optional.of(existing));

		CanCreateResponse response = diaryService.canCreate(userId, date);

		assertFalse(response.isCanCreate());
		assertEquals("해당 날짜에 이미 일기가 있습니다.", response.getReason());
		verify(diaryRepository, never()).countByUserIdAndCreatedAtAfter(any(), any());
	}

	@Test
	void 오늘_생성_횟수가_5회_이상이면_생성불가를_반환한다() {
		UUID userId = UUID.randomUUID();
		LocalDate date = LocalDate.of(2026, 9, 10);

		when(diaryRepository.findByUserIdAndEntryDateAndStatus(userId, date, DiaryStatus.ACTIVE))
				.thenReturn(Optional.empty());
		when(diaryRepository.countByUserIdAndCreatedAtAfter(eq(userId), any(LocalDateTime.class)))
				.thenReturn(5L);

		CanCreateResponse response = diaryService.canCreate(userId, date);

		assertFalse(response.isCanCreate());
		assertEquals("하루 생성 횟수 5회 초과", response.getReason());
	}

	@Test
	void 조건을_모두_만족하면_생성가능을_반환한다() {
		UUID userId = UUID.randomUUID();
		LocalDate date = LocalDate.of(2026, 9, 10);

		when(diaryRepository.findByUserIdAndEntryDateAndStatus(userId, date, DiaryStatus.ACTIVE))
				.thenReturn(Optional.empty());
		when(diaryRepository.countByUserIdAndCreatedAtAfter(eq(userId), any(LocalDateTime.class)))
				.thenReturn(2L);

		CanCreateResponse response = diaryService.canCreate(userId, date);

		assertTrue(response.isCanCreate());
		assertNull(response.getReason());
	}

	// ----- getAllTrashedDiaries -----

	@Test
	void TRASHED_상태의_일기_목록을_반환한다() {
		UUID userId = UUID.randomUUID();
		Diary trashed1 = Mockito.mock(Diary.class);
		Diary trashed2 = Mockito.mock(Diary.class);

		when(diaryRepository.findByUserIdAndStatus(userId, DiaryStatus.TRASHED))
				.thenReturn(List.of(trashed1, trashed2));

		List<DiaryResponse> responses = diaryService.getAllTrashedDiaries(userId);

		assertEquals(2, responses.size());
	}

	@Test
	void TRASHED_일기가_없으면_빈_목록을_반환한다() {
		UUID userId = UUID.randomUUID();

		when(diaryRepository.findByUserIdAndStatus(userId, DiaryStatus.TRASHED))
				.thenReturn(List.of());

		List<DiaryResponse> responses = diaryService.getAllTrashedDiaries(userId);

		assertTrue(responses.isEmpty());
	}
}
