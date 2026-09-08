package com.tada.tada.curator.event;

import com.tada.tada.curator.exception.ExtractionValidationException;
import com.tada.tada.curator.service.MentionExtractionProcessor;
import com.tada.tada.global.event.MentionExtractedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/*
 * Curator 후처리는 일기 최종 저장의 일부다.
 *
 * Diary 저장 @Transactional 내부에서 발행된 MentionExtractedEvent 를
 * 일반 동기 @EventListener 로 즉시 받아 같은 트랜잭션에서 처리한다.
 *
 * 여기서 예외가 나면 삼키지 않고 그대로 위로 던진다.
 * 그래야 Diary, Sticker, Candidate, Relation, DiaryPerson, PersonAggregate 를
 * 포함한 저장 트랜잭션 전체가 rollback 되어
 * "그 일기는 저장되지 않은 상태" 로 남는다.
 *
 * 금지:
 *   - @TransactionalEventListener(AFTER_COMMIT) 로 커밋 이후 실행
 *   - Processor 를 REQUIRES_NEW 로 분리
 *   - 예외를 catch 해서 저장 성공으로 처리
 * 위 셋 중 하나라도 하면 Curator 가 실패해도 Diary 만 commit 되어
 * 인물 정보가 비어 있는 일기가 남는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MentionExtractedEventListener {

	private final MentionExtractionProcessor mentionExtractionProcessor;

	/*
	 * 로그를 남기고 반드시 다시 던진다.
	 *
	 * GlobalExceptionHandler 의 catch-all 은 메시지 없이 500 만 응답하고
	 * 아무 로그도 남기지 않는다. 여기서 남기지 않으면
	 * 저장 실패의 원인이 어디에도 기록되지 않는다.
	 *
	 * 로그만 남기고 삼키면 안 된다. 다시 던져야
	 * 저장 트랜잭션 전체가 rollback 된다.
	 */
	@EventListener
	public void handle(
			MentionExtractedEvent event
	) {
		try {
			mentionExtractionProcessor.process(event);
		} catch (ExtractionValidationException e) {
			/*
			 * 사용자에게 나가는 메시지는 한국어 한 줄이라
			 * 어떤 검증이 깨졌는지는 여기서만 남는다.
			 */
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
