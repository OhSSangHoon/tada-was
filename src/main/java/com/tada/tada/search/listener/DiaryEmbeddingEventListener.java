package com.tada.tada.search.listener;

import com.tada.tada.domain.diary.entity.Diary;
import com.tada.tada.global.event.DiaryCreatedEvent;
import com.tada.tada.global.event.DiaryUpdatedEvent;
import com.tada.tada.search.repository.SearchRepository;
import com.tada.tada.search.service.VoyageAIEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/*
	DiaryEmbeddingEventListener - 일기 저장/수정 시 자동으로 임베딩을 생성/갱신 하는 리스너
	
	- DiaryCreatedEvent 구독 : 일기가 새로 저장되면 Votage AI로 임베딩 생성 후 diaries.embedding에 저장
	- DiaryUpdatedEvent 구독 : 일가 본문이 실제로 수정되면 재임베딩
	
	**주의**
	- DiaryCreatedEvent(diaryId, userId)에는 content가 없어서, diartId로 DB에서 다시 조회해아함
	- DiaryUpdatedEvent(diaryId, userId, oldContent, newContent)에는 newContent가 바로 들어 있어서 DB 재조회 없이 바로 재임베딩 가능
	- Diary 엔티티를 fetch해서 save()하는 대신, SearchRepository.updateEmbedding()으로 네이티브 UPDATE 쿼리를 사용함
	Diary.embedding 필드가 아직 float[]로 매핑되어 있어, entity를 통해 save()하면 pgvector 타입과 안 맞을 수 있어 이 방식으로 우회
	- CustomException은 Controller 요청에 대한 응답용이라 여기서는 사용하지 않음
 */
@Component
@RequiredArgsConstructor
public class DiaryEmbeddingEventListener {
	
	private final SearchRepository searchRepository;
	private final VoyageAIEmbeddingService voyageAIEmbeddingService;
	
	/*
		일기 생성 이벤트 처리 - 새 임베딩 생성
		@param event diaryId, userId만 담고 있음
	 */
	@EventListener
	public void handleDiaryCreated(DiaryCreatedEvent event) {
		
		// content가 이벤트에 없으므로, diaryId로 DB에서 일기를 다시 조회
		//	(SearchRepository가 JpaRepository<Diary, UUID>를 상속하므로 findById 바로 사용 가능)
		Diary diary = searchRepository.findById(event.diaryId())
				.orElseThrow(() -> new IllegalStateException(
						"임베딩 생성 대상 일기를 찾을 수 없습니다. diaryId=" + event.diaryId()
				));
		
		// 일기 본문을 임베딩 벡터로 변환 (1024차원 float 배열)
		float[] embedding = voyageAIEmbeddingService.embed(diary.getContent());
		String embeddingString = Arrays.toString(embedding);
		
		// 네이티브 UPDATE 쿼리로 embedding 컬럼 갱신
		searchRepository.updateEmbedding(event.diaryId(), embeddingString);
	}
	
	/*
		일기 수정 이벤트 처리 - 재임베딩
		@param event diaryId, userId, oldContent, newContent를 담고 있음
	 */
	@EventListener
	public void handleDiaryUpdated(DiaryUpdatedEvent event) {
		
		// newContent가 이벤트에 이미 있으므로, DB 재조회 없이 바로 임베딩 계산
		float[] embedding = voyageAIEmbeddingService.embed(event.newContent());
		String embeddingString = Arrays.toString(embedding);
		
		// 네이티브 UPDATE 쿼리로 embedding 컬럼 갱신
		searchRepository.updateEmbedding(event.diaryId(), embeddingString);
	}
}
