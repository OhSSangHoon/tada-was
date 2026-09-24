package com.tada.tada.diary.service;

import com.tada.tada.diary.dto.DiaryUpdateForm;
import com.tada.tada.diary.entity.Diary;
import com.tada.tada.diary.repository.DiaryRepository;
import com.tada.tada.global.event.DiaryUpdatedEvent;
import com.tada.tada.global.event.MentionExtractedEvent;
import com.tada.tada.global.event.dto.ExtractionResult;
import com.tada.tada.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DiaryContentUpdaterTest {

	private DiaryRepository diaryRepository;
	private ApplicationEventPublisher eventPublisher;
	private DiaryContentUpdater diaryContentUpdater;

	@BeforeEach
	void setUp() {
		diaryRepository = Mockito.mock(DiaryRepository.class);
		eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
		diaryContentUpdater = new DiaryContentUpdater(diaryRepository, eventPublisher);
	}

	@Test
	void 수정_대상이_없으면_404를_던진다() {
		UUID userId = UUID.randomUUID();
		UUID diaryId = UUID.randomUUID();
		DiaryUpdateForm form = new DiaryUpdateForm();

		when(diaryRepository.findById(diaryId)).thenReturn(Optional.empty());

		CustomException exception = assertThrows(
				CustomException.class,
				() -> diaryContentUpdater.apply(userId, diaryId, form, null)
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

		when(diaryRepository.findById(diaryId)).thenReturn(Optional.of(diary));
		when(diary.getUserId()).thenReturn(ownerId);

		CustomException exception = assertThrows(
				CustomException.class,
				() -> diaryContentUpdater.apply(userId, diaryId, form, null)
		);
		assertEquals(403, exception.getStatusCode());
	}

	@Test
	void TRASHED_상태의_일기를_수정하려하면_404를_던진다() {
		UUID userId = UUID.randomUUID();
		UUID diaryId = UUID.randomUUID();
		Diary diary = Mockito.mock(Diary.class);
		DiaryUpdateForm form = new DiaryUpdateForm();

		when(diaryRepository.findById(diaryId)).thenReturn(Optional.of(diary));
		when(diary.getUserId()).thenReturn(userId);
		when(diary.isActive()).thenReturn(false);

		CustomException exception = assertThrows(
				CustomException.class,
				() -> diaryContentUpdater.apply(userId, diaryId, form, null)
		);
		assertEquals(404, exception.getStatusCode());
	}

	@Test
	void extractionResult가_null이면_필드만_반영하고_이벤트는_발행하지_않는다() {
		UUID userId = UUID.randomUUID();
		UUID diaryId = UUID.randomUUID();
		Diary diary = Mockito.mock(Diary.class);
		DiaryUpdateForm form = new DiaryUpdateForm();
		form.setTitle("새 제목");
		form.setWeather("SUNNY");
		form.setContent("기존 본문");

		when(diaryRepository.findById(diaryId)).thenReturn(Optional.of(diary));
		when(diary.getUserId()).thenReturn(userId);
		when(diary.isActive()).thenReturn(true);
		when(diary.getContent()).thenReturn("기존 본문");

		diaryContentUpdater.apply(userId, diaryId, form, null);

		verify(diary).update("새 제목", "SUNNY", "기존 본문");
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void extractionResult가_있으면_MentionExtractedEvent와_DiaryUpdatedEvent를_둘다_발행한다() {
		UUID userId = UUID.randomUUID();
		UUID diaryId = UUID.randomUUID();
		Diary diary = Mockito.mock(Diary.class);
		ExtractionResult extractionResult = new ExtractionResult(List.of(), List.of(), List.of());
		DiaryUpdateForm form = new DiaryUpdateForm();
		form.setTitle("제목");
		form.setContent("새로운 본문");

		when(diaryRepository.findById(diaryId)).thenReturn(Optional.of(diary));
		when(diary.getUserId()).thenReturn(userId);
		when(diary.isActive()).thenReturn(true);
		when(diary.getContent()).thenReturn("기존 본문");

		diaryContentUpdater.apply(userId, diaryId, form, extractionResult);

		verify(diary).update("제목", null, "새로운 본문");
		verify(eventPublisher).publishEvent(new MentionExtractedEvent(diaryId, userId, extractionResult));
		verify(eventPublisher).publishEvent(new DiaryUpdatedEvent(diaryId, userId, "기존 본문", "새로운 본문"));
	}
}
