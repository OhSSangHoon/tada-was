package com.tada.tada.curator.event;

import com.tada.tada.curator.service.MentionExtractionProcessor;
import com.tada.tada.global.event.MentionExtractedEvent;
import com.tada.tada.global.event.dto.ExtractionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
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

	/*
	 * Curator 는 일기 최종 저장 트랜잭션의 일부다.
	 *
	 * 예외를 삼키면 Curator 가 실패해도 Diary 만 commit 되어
	 * 인물 정보가 비어 있는 일기가 남는다.
	 * 반드시 위로 전파해서 저장 트랜잭션 전체를 rollback 시켜야 한다.
	 */
	@Test
	void processor_실패를_삼키지_않고_그대로_전파한다() {
		doThrow(new IllegalStateException("boom"))
				.when(mentionExtractionProcessor)
				.process(any());

		IllegalStateException thrown =
				assertThrows(
						IllegalStateException.class,
						() -> listener.handle(event())
				);

		org.junit.jupiter.api.Assertions.assertEquals(
				"boom",
				thrown.getMessage()
		);
	}

	/*
	 * 로그를 남기더라도 반드시 다시 던져야 한다.
	 * 로그만 남기고 삼키면 저장 트랜잭션이 commit 되어 정책이 깨진다.
	 */
	@Test
	void 검증_실패도_그대로_전파한다() {
		doThrow(new IllegalArgumentException("invalid"))
				.when(mentionExtractionProcessor)
				.process(any());

		assertThrows(
				IllegalArgumentException.class,
				() -> listener.handle(event())
		);
	}
}
