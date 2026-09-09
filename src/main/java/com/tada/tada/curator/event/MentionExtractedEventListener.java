package com.tada.tada.curator.event;

import com.tada.tada.curator.exception.ExtractionValidationException;
import com.tada.tada.curator.service.MentionExtractionProcessor;
import com.tada.tada.global.event.MentionExtractedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/*
 * Diary 저장 트랜잭션 안에서 동기 처리하고 예외는 삼키지 않고 그대로 던진다 (전체 rollback).
 * 금지: @TransactionalEventListener(AFTER_COMMIT), Processor를 REQUIRES_NEW로 분리, 예외 catch 후 성공 처리.
 * 셋 중 하나라도 하면 Curator가 실패해도 Diary만 commit되어 인물 정보 없는 일기가 남는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MentionExtractedEventListener {

	private final MentionExtractionProcessor mentionExtractionProcessor;

	/*
	 * catch-all 500 응답은 로그를 안 남기므로 여기서 남기고 반드시 재던진다 (삼키면 저장 rollback이 깨진다).
	 */
	@EventListener
	public void handle(
			MentionExtractedEvent event
	) {
		try {
			mentionExtractionProcessor.process(event);
		} catch (ExtractionValidationException e) {
			log.error(
					"Curator extraction validation failed. "
							+ "diaryId={}, userId={}, detail={}",
					event == null ? null : event.diaryId(),
					event == null ? null : event.userId(),
					e.getDetail()
			);

			throw e;

		} catch (RuntimeException e) {
			log.error(
					"Curator mention processing failed. "
							+ "diaryId={}, userId={}. "
							+ "저장 트랜잭션 전체를 rollback 한다.",
					event == null ? null : event.diaryId(),
					event == null ? null : event.userId(),
					e
			);

			throw e;
		}
	}
}
