package com.tada.tada.diary.scheduler;

import com.tada.tada.diary.service.StickerCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/*
 * 미참조 스티커 오브젝트를 매일 자동으로 정리한다 (상훈 PR #32 리뷰 반영).
 * 판단/삭제 로직은 StickerCleanupService에 있고, 이 클래스는 주기 트리거만 담당한다
 * (TrashCleanupScheduler와 같은 패턴 - 로직이 두 곳으로 갈라지지 않게).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StickerCleanupScheduler {

	private final StickerCleanupService stickerCleanupService;

	@Scheduled(cron = "0 30 4 * * *")
	public void purgeOrphanedStickerObjects() {
		try {
			stickerCleanupService.cleanupOrphanedObjects();
		} catch (RuntimeException e) {
			log.error("미참조 스티커 오브젝트 정리 실패", e);
		}
	}
}
