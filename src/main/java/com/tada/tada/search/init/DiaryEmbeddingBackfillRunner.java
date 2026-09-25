package com.tada.tada.search.init;

import com.tada.tada.diary.entity.Diary;
import com.tada.tada.global.exception.CustomException;
import com.tada.tada.search.repository.SearchRepository;
import com.tada.tada.search.service.VoyageAIEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DiaryEmbeddingBackfillRunner implements CommandLineRunner {
	
	private final SearchRepository searchRepository;
	private final VoyageAIEmbeddingService voyageAIEmbeddingService;
	
	private static final long REQUEST_INTERVAL_MS = 1000;
	private static final long RATE_LIMIT_WAIT_MS = 20_000; // 429일 때 대기 시간
	private static final int MAX_RETRIES = 3;
	
	@Override
	public void run(String... args) {
		
		List<UUID> diaryIds = searchRepository.findDiaryIdsWithoutEmbedding();
		
		if (diaryIds.isEmpty()) {
			System.out.println("[DiaryEmbeddingBackfillRunner] 임베딩 채울 일기 없음 (전부 완료 상태)");
			return;
		}
		
		System.out.println("[DiaryEmbeddingBackfillRunner] " + diaryIds.size() + "개 임베딩 시작");
		
		int successCount = 0;
		int failCount = 0;
		
		for (UUID diaryId : diaryIds) {
			Diary diary = searchRepository.findById(diaryId).orElse(null);
			if (diary == null) {
				continue;
			}
			
			boolean success = false;
			for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
				try {
					float[] embedding = voyageAIEmbeddingService.embed(diary.getContent());
					String embeddingString = Arrays.toString(embedding);
					searchRepository.updateEmbedding(diaryId, embeddingString);
					success = true;
					break;
				} catch (CustomException e) {
					if (e.getStatusCode() == 429) {
						System.out.println("[DiaryEmbeddingBackfillRunner] diaryId=" + diaryId
								+ " 429 발생, " + (RATE_LIMIT_WAIT_MS / 1000) + "초 대기 후 재시도 (" + attempt + "/" + MAX_RETRIES + ")");
						sleep(RATE_LIMIT_WAIT_MS);
					} else {
						System.out.println("[DiaryEmbeddingBackfillRunner] diaryId=" + diaryId + " 임베딩 실패: " + e.getMessage());
						break; // 429가 아니면 재시도 의미 없음
					}
				}
			}
			
			if (success) {
				successCount++;
			} else {
				failCount++;
			}
			
			sleep(REQUEST_INTERVAL_MS);
		}
		
		System.out.println("[DiaryEmbeddingBackfillRunner] 임베딩 채우기 완료 (성공 " + successCount + ", 실패 " + failCount + ")");
	}
	
	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}