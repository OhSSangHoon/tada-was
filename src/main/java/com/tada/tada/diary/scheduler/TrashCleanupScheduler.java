package com.tada.tada.diary.scheduler;

import com.tada.tada.diary.entity.Diary;
import com.tada.tada.diary.entity.DiaryStatus;
import com.tada.tada.diary.repository.DiaryRepository;
import com.tada.tada.diary.service.DiaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/*
 * 휴지통(TRASHED) 30일 경과 일기를 매일 자동으로 영구삭제한다 (REQ-F-205).
 * permanentlyDeleteDiary()를 재사용 - 수동 영구삭제 API와 로직이 두 곳으로 갈라지지 않게 함.
 *
 * cutoff를 Diary.trash()가 deletedAt을 쓰는 방식(LocalDateTime.now(), zone 미지정)과 똑같이
 * zone 없이 계산함 - KST로 계산하면 서버 JVM 기본 zone과 어긋나서 오히려 새로운 오차가 생김.
 * (entryDate 쪽 타임존 이슈는 별도로 상훈에게 전달된 사안이라 여기서 같이 고치지 않음)
 *
 * 한 번의 실패가 나머지 대상까지 막지 않도록 개별 diary 단위로 try-catch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrashCleanupScheduler {

	private static final int TRASH_RETENTION_DAYS = 30;

	private final DiaryRepository diaryRepository;
	private final DiaryService diaryService;

	@Scheduled(cron = "0 0 4 * * *")
	public void purgeExpiredTrash() {
		LocalDateTime cutoff = LocalDateTime.now().minusDays(TRASH_RETENTION_DAYS);
		List<Diary> expired = diaryRepository.findByStatusAndDeletedAtBefore(DiaryStatus.TRASHED, cutoff);

		for (Diary diary : expired) {
			try {
				diaryService.permanentlyDeleteDiary(diary.getUserId(), diary.getId());
			} catch (RuntimeException e) {
				log.error("휴지통 자동 영구삭제 실패. diaryId={}", diary.getId(), e);
			}
		}
	}
}
