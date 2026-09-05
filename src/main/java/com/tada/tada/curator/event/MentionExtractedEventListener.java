package com.tada.tada.curator.event;

import com.tada.tada.curator.service.MentionExtractionProcessor;
import com.tada.tada.global.event.MentionExtractedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/*
 * Curator 후처리는 Diary 저장의 성공 여부를 좌우하지 않는다.
 *
 * Diary transaction 이 commit 된 뒤에 이벤트를 받고,
 * 실제 처리는 MentionExtractionProcessor 의 REQUIRES_NEW transaction 에서 한다.
 * Processor 가 실패하면 그 transaction 만 rollback 되고
 * 여기서 잡아 로그만 남긴다.
 *
 * 이 PR 에서는 @Async, 자동 재시도, 메시지 큐를 도입하지 않는다.
 * 실패한 일기는 본문 수정 등으로 이벤트가 다시 발행될 때 복구된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MentionExtractedEventListener {

	private final MentionExtractionProcessor mentionExtractionProcessor;

	/*
	 * fallbackExecution 이 없으면 트랜잭션 밖에서 발행된 이벤트는
	 * 아무 로그도 없이 그냥 버려진다.
	 *
	 * 현재 이 이벤트를 발행하는 코드가 아직 없으므로,
	 * 나중에 발행부를 추가하는 사람이 @Transactional 안에서
	 * 발행하지 않아도 Curator 가 조용히 멈추지 않도록 켜 둔다.
	 *
	 * 트랜잭션 안에서 발행된 경우에는 그대로 commit 이후에만 실행되고,
	 * 롤백되면 실행되지 않는다.
	 */
	@TransactionalEventListener(
			phase = TransactionPhase.AFTER_COMMIT,
			fallbackExecution = true
	)
	public void handle(
			MentionExtractedEvent event
	) {
		try {
			mentionExtractionProcessor.process(event);
		} catch (Exception e) {
			/*
			 * Diary 는 이미 commit 됐으므로 예외를 밖으로 던지지 않는다.
			 * 던져도 되돌릴 대상이 없고 AFTER_COMMIT 콜백에서 삼켜진다.
			 */
			log.error(
					"Curator mention processing failed. diaryId={}, userId={}",
					event == null ? null : event.diaryId(),
					event == null ? null : event.userId(),
					e
			);
		}
	}
}
