package com.tada.tada.curator.event;

import com.tada.tada.curator.service.DiaryStatusChangeService;
import com.tada.tada.global.event.DiaryRestoredEvent;
import com.tada.tada.global.event.DiaryTrashedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/*
 * Trash/Restore 트랜잭션 안에서 동기 처리하고 예외를 삼키지 않는다.
 * 통계가 틀린 채 성공하는 것보다 실패 후 재시도가 안전하다 (재계산은 ACTIVE 원본 기준이라 몇 번을 돌려도 같은 값).
 * Candidate/Relation/DiaryPerson은 유지한다 (명세 16.1, 16.2).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiaryStatusEventListener {

	private final DiaryStatusChangeService diaryStatusChangeService;

	@EventListener
	public void handleTrashed(
			DiaryTrashedEvent event
	) {
		handle(
				"trash",
				event == null ? null : event.diaryId(),
				event == null ? null : event.userId()
		);
	}

	@EventListener
	public void handleRestored(
			DiaryRestoredEvent event
	) {
		handle(
				"restore",
				event == null ? null : event.diaryId(),
				event == null ? null : event.userId()
		);
	}

	private void handle(
			String action,
			UUID diaryId,
			UUID userId
	) {
		try {
			diaryStatusChangeService
					.recalculateAffectedPersons(
							diaryId,
							userId
					);
		} catch (RuntimeException e) {
			log.error(
					"Curator aggregate recalculation failed. "
							+ "action={}, diaryId={}, userId={}. "
							+ "상태 변경 트랜잭션 전체를 rollback 한다.",
					action,
					diaryId,
					userId,
					e
			);

			throw e;
		}
	}
}
