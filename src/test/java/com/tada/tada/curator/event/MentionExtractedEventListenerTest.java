package com.tada.tada.curator.event;

import com.tada.tada.curator.service.DiaryPersonService;
import com.tada.tada.curator.service.MentionCandidatePersonRefService;
import com.tada.tada.curator.service.MentionCandidateService;
import com.tada.tada.curator.service.PersonAggregateService;
import com.tada.tada.curator.validation.ExtractionResultValidator;
import com.tada.tada.diary.entity.Diary;
import com.tada.tada.diary.repository.DiaryRepository;
import com.tada.tada.global.event.MentionExtractedEvent;
import com.tada.tada.global.event.dto.ExtractionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MentionExtractedEventListenerTest {

	private DiaryRepository diaryRepository;
	private ExtractionResultValidator extractionResultValidator;
	private MentionCandidateService mentionCandidateService;
	private MentionCandidatePersonRefService relationService;
	private DiaryPersonService diaryPersonService;
	private PersonAggregateService personAggregateService;

	private MentionExtractedEventListener listener;

	@BeforeEach
	void setUp() {
		diaryRepository =
				Mockito.mock(DiaryRepository.class);

		extractionResultValidator =
				Mockito.mock(ExtractionResultValidator.class);

		mentionCandidateService =
				Mockito.mock(MentionCandidateService.class);

		relationService =
				Mockito.mock(
						MentionCandidatePersonRefService.class
				);

		diaryPersonService =
				Mockito.mock(DiaryPersonService.class);

		personAggregateService =
				Mockito.mock(PersonAggregateService.class);

		listener =
				new MentionExtractedEventListener(
						diaryRepository,
						extractionResultValidator,
						mentionCandidateService,
						relationService,
						diaryPersonService,
						personAggregateService
				);
	}

	@Test
	void 이미_처리된_일기의_중복_이벤트는_다시_처리하지_않는다() {
		UUID diaryId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		Diary diary =
				Diary.builder()
						.userId(userId)
						.entryDate(LocalDate.now())
						.title("오늘")
						.weather(null)
						.content("민수를 만났다")
						.build();

		ExtractionResult extractionResult =
				new ExtractionResult(
						List.of(),
						List.of(),
						List.of()
				);

		MentionExtractedEvent event =
				new MentionExtractedEvent(
						diaryId,
						userId,
						extractionResult
				);

		when(
				diaryRepository.findById(diaryId)
		).thenReturn(
				Optional.of(diary)
		);

		when(
				mentionCandidateService.hasCandidates(
						diaryId
				)
		).thenReturn(true);

		listener.handle(event);

		verify(
				mentionCandidateService
		).hasCandidates(diaryId);

		verify(
				extractionResultValidator,
				never()
		).validate(
				Mockito.any(),
				Mockito.any()
		);

		verifyNoInteractions(
				relationService,
				diaryPersonService,
				personAggregateService
		);
	}
}