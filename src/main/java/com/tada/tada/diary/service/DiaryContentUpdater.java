package com.tada.tada.diary.service;

import com.tada.tada.diary.dto.DiaryResponse;
import com.tada.tada.diary.dto.DiaryUpdateForm;
import com.tada.tada.diary.entity.Diary;
import com.tada.tada.diary.repository.DiaryRepository;
import com.tada.tada.global.event.DiaryUpdatedEvent;
import com.tada.tada.global.event.MentionExtractedEvent;
import com.tada.tada.global.event.dto.ExtractionResult;
import com.tada.tada.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/*
 * updateDiary()의 DB 반영 + 이벤트 발행 부분만 별도 빈으로 분리했다.
 *
 * DiaryService.updateDiary()가 n8n 재추출(외부 I/O)을 트랜잭션 밖에서 먼저 끝낸 뒤 이 메서드를
 * 호출하는데, 같은 클래스 안에서 this.xxx()로 호출하면 Spring 프록시가 @Transactional을 못 걸어서
 * (self-invocation) 트랜잭션이 아예 안 열리는 문제가 생긴다 - 그래서 진짜 별도 빈으로 뺐다.
 *
 * extractionResult가 null이면(=본문이 안 바뀜) 재추출 없이 필드만 갱신하고 이벤트는 안 쏜다.
 */
@Service
@RequiredArgsConstructor
class DiaryContentUpdater {

	private final DiaryRepository diaryRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public DiaryResponse apply(UUID userId, UUID diaryId, DiaryUpdateForm form, ExtractionResult extractionResult) {
		Diary diary = diaryRepository.findById(diaryId)
				.orElseThrow(() -> new CustomException("일기를 찾을 수 없습니다.", 404));

		if (!diary.getUserId().equals(userId)) {
			throw new CustomException("접근 권한이 없습니다.", 403);
		}

		if (!diary.isActive()) {
			throw new CustomException("일기를 찾을 수 없습니다.", 404);
		}

		String oldContent = diary.getContent();
		diary.update(form.getTitle(), form.getWeather(), form.getContent());

		if (extractionResult != null) {
			eventPublisher.publishEvent(
					new MentionExtractedEvent(diaryId, userId, extractionResult)
			);
			eventPublisher.publishEvent(
					new DiaryUpdatedEvent(diaryId, userId, oldContent, form.getContent())
			);
		}

		return DiaryResponse.from(diary);
	}
}
