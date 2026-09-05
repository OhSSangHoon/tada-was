package com.tada.tada.curator.event;

import com.tada.tada.curator.service.MentionExtractionProcessor;
import com.tada.tada.global.event.MentionExtractedEvent;
import com.tada.tada.global.event.dto.ExtractionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class MentionExtractedEventListenerTest {

	private MentionExtractionProcessor mentionExtractionProcessor;
	private MentionExtractedEventListener listener;

	@BeforeEach
	void setUp() {
		mentionExtractionProcessor =
				Mockito.mock(MentionExtractionProcessor.class);

		listener =
				new MentionExtractedEventListener(
						mentionExtractionProcessor
				);
	}

	private MentionExtractedEvent event() {
		return new MentionExtractedEvent(
				UUID.randomUUID(),
				UUID.randomUUID(),
				new ExtractionResult(
						List.of(),
						List.of(),
						List.of()
				)
		);
	}

	@Test
	void 이벤트를_processor에_위임한다() {
		MentionExtractedEvent event = event();

		listener.handle(event);

		verify(mentionExtractionProcessor).process(event);
	}

	@Test
	void processor_실패가_diary_트랜잭션으로_전파되지_않는다() {
		/*
		 * Diary 는 이미 commit 된 뒤이므로
		 * Curator 실패를 밖으로 던지지 않고 로그만 남긴다.
		 */
		doThrow(new IllegalStateException("boom"))
				.when(mentionExtractionProcessor)
				.process(any());

		assertDoesNotThrow(
				() -> listener.handle(event())
		);
	}

	@Test
	void 검증_실패도_밖으로_전파되지_않는다() {
		doThrow(new IllegalArgumentException("invalid"))
				.when(mentionExtractionProcessor)
				.process(any());

		assertDoesNotThrow(
				() -> listener.handle(event())
		);
	}
}
