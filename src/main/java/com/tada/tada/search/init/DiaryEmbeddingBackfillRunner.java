package com.tada.tada.search.init;

/*
	CommandLineRunner
	- Spring Boot가 애플리케이션 완전히 뜬 직후 run()을 자동으로 한번 호출해주는 인터페이스
 */

import com.tada.tada.domain.diary.entity.Diary;
import com.tada.tada.search.repository.SearchRepository;
import com.tada.tada.search.service.VoyageAIEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DiaryEmbeddingBackfillRunner implements CommandLineRunner {
	
	private final SearchRepository searchRepository;
	private final VoyageAIEmbeddingService voyageAIEmbeddingService;
	
	@Override
	public void run(String... args) {
		
		// embedding이 아직 없는 일기 id 목록 조회
		List<UUID> diaryIds = searchRepository.findDiaryIdsWithoutEmbedding();
		
		if (diaryIds.isEmpty()) {
			System.out.println("[DiaryEmbeddingBackfillRunner] 임베딩 채울 일기 없음 (전부 완료 상태)");
			return;
		}
		
		System.out.println("[DiaryEmbeddingBackfillRunner]" + diaryIds.size() + "개 임베딩 시작");
		
		// 하나씩 조회 -> 임베딩 계산 -> 저장
		for (UUID diaryId : diaryIds) {
			Diary diary = searchRepository.findById(diaryId).orElse(null);
			if (diary == null) {
				continue;	// 삭제되면 건너뜀
			}
			
			float[] embedding = voyageAIEmbeddingService.embed(diary.getContent());
			String embeddingString = Arrays.toString(embedding);
			
			searchRepository.updateEmbedding(diaryId, embeddingString);
		}
		System.out.println("[DiaryEmbeddingBackfillRunner] 임베딩 채우기 완료");
	}
}
