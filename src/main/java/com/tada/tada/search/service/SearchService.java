package com.tada.tada.search.service;

import com.tada.tada.search.dto.SearchResultProjection;
import com.tada.tada.search.dto.SearchResultResponse;
import com.tada.tada.search.repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/*
	SearchService - RAG 검색 비즈니스 로직
	
	역할:
	사용자가 입력한 자연어 검색어를 임베딩 벡터로 변환 (VoyageAIEmbeddingService 위임)
	변환된 벡터로 SearchRepository를 통해 유사 일기 검색
	결과를 Controller에 반환
 */
@Service
@RequiredArgsConstructor
public class SearchService {
	
	// SearchRepository 주입 - pgvector 검색 쿼리 실행 담당
	private final SearchRepository searchRepository;
	
	// VoyageAIEmbeddingService 주입 - 텍스트 -> 벡터 변환 담당
	private final VoyageAIEmbeddingService voyageAIEmbeddingService;
	
	
	/*
		자연어 검색으로 유사한 일기 페이지네이션과 함께 검색
		
		동작 순서
		- queryText를 임베딩 벡터로 변환
		- float[] 벡터를 pgvector가 이해하는 문자열 형태로 변환
		- SearchRepository의 네이티브 쿼리로 코사인 거리 기반 검색 수행
		- 결과를 Page 객체로 반환
		
		@param queryText 사용자가 입력한 자연어 검색
		@param pageable 페이지 정보
		@return 유사도순으로 정렬된 일기 Page 객체
	 */
	public Page<SearchResultResponse> search(String queryText, Pageable pageable) {        // 반환 타입 DTO 변경
		
		// 쿼리 텍스트를 벡터로 변환 (1024차원 float 배열)
		float[] embedding = voyageAIEmbeddingService.embed(queryText);
		
		// float[] 배열을 문자열로 변환 -> pgvector 쿼리 파라미터로 사용
		String embeddingString = Arrays.toString(embedding);
		
		Page<SearchResultProjection> diaryPage = searchRepository.findSimilarDiariesWithPagination(embeddingString, pageable);
		
		return diaryPage.map(this::toSearchResultResponse);    // Entity DTO 변환 추가
	}
	private SearchResultResponse toSearchResultResponse(SearchResultProjection  projection) {
		return new SearchResultResponse(
				projection.getId(), projection.getEntryDate(), projection.getTitle(),
				projection.getWeather(), projection.getContent(), projection.getCreatedAt()
		);
	}
}
