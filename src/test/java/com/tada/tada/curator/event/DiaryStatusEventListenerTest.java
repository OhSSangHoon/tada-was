package com.tada.tada.curator.event;

import com.tada.tada.curator.service.DiaryStatusChangeService;
import com.tada.tada.global.event.DiaryRestoredEvent;
import com.tada.tada.global.event.DiaryTrashedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class DiaryStatusEventListenerTest {

	private DiaryStatusChangeService diaryStatusChangeService;
	private DiaryStatusEventListener listener;

	@BeforeEach
	void setUp() {
		diaryStatusChangeService =
				Mockito.mock(DiaryStatusChangeService.class);

		listener =
				new DiaryStatusEventListener(
						diaryStatusChangeService
				);
	}

	@Test
	void Trash_이벤트를_재계산으로_위임한다() {
		UUID diaryId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		listener.handleTrashed(
				new DiaryTrashedEvent(diaryId, userId)
		);

		verify(diaryStatusChangeService)
				.recalculateAffectedPersons(diaryId, userId);
	}

	@Test
	void Restore_이벤트를_같은_재계산으로_위임한다() {
		UUID diaryId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		listener.handleRestored(
				new DiaryRestoredEvent(diaryId, userId)
		);

		verify(diaryStatusChangeService)
				.recalculateAffectedPersons(diaryId, userId);
	}

	/*
	 * 통계가 틀린 채로 Trash 만 성공하면 안 된다.
	 * 예외를 전파해 상태 변경 트랜잭션 전체를 rollback 시킨다.
	 */
	@Test
	void 재계산_실패를_삼키지_않고_전파한다() {
		doThrow(new IllegalStateException("boom"))
				.when(diaryStatusChangeService)
				.recalculateAffectedPersons(any(), any());

		assertThrows(
				IllegalStateException.class,
				() -> listener.handleTrashed(
						new DiaryTrashedEvent(
								UUID.randomUUID(),
								UUID.randomUUID()
						)
				)
		);

		assertThrows(
				IllegalStateException.class,
				() -> listener.handleRestored(
						new DiaryRestoredEvent(
								UUID.randomUUID(),
								UUID.randomUUID()
						)
				)
		);
	}
}
