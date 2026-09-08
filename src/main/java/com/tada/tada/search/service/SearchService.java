package com.tada.tada.search.service;

import com.tada.tada.global.exception.CustomException;
import com.tada.tada.search.dto.SearchResultProjection;
import com.tada.tada.search.dto.SearchResultResponse;
import com.tada.tada.search.repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;

/*
	SearchService - RAG 검색 비즈니스 로직
	
	역할:
	사용자가 입력한 자연어 검색어를 임베딩 벡터로 변환 (VoyageAIEmbeddingService 위임)
	변환된 벡터로 SearchRepository를 통해 유사 일기 검색 (본인 일기로 제한)
	결과를 Controller에 반환
	
	예외 처리:
	- query가 비어있으면 400 (임베딩 호출 자체를 막음 - 불필요한 외부 API 호출 방지)
	- 임베딩 생성(Votage AI 호출) 실패 시 503 (외부 서비스 장애로 간주)
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
		- query 공백 여부 검증
		- queryText를 임베딩 벡터로 변환 (실패 시 CustomException)
		- float[] 벡터를 pgvector가 이해하는 문자열 형태로 변환
		- SearchRepository의 네이티브 쿼리로 본인 일기 중 코사인 거리 기반 검색
		- 결과를 Page 객체로 반환
		
		@param userId 검색을 요청한 로그인 사용자 ID
		@param queryText 사용자가 입력한 자연어 검색어
		@param pageable 페이지 정보
		@return 유사도순으로 정렬된 본인 일기 Page 객체
	 */
	public Page<SearchResultResponse> search(UUID userId, String queryText, Pageable pageable) {
		
		// 검색어가 비어있으면 임베딩 계산 (외부 API 호출)까지 갈 필요 없이 바로 막음
		if (queryText == null || queryText.isBlank()) {
			throw new CustomException("검색어를 입력해주세요.", 400);
		}
		
		// 쿼리 텍스트를 벡터로 변환 (1024차원 float 배열)
		float[] embedding;
		try {
			embedding = voyageAIEmbeddingService.embed(queryText);
		} catch (Exception e) {
			// Voyage AI 호출 실패(타임아웃 등) - 외부 서비스 장애
			throw new CustomException("검색어 임베딩 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.", 503);
		}
		
		// float[] 배열을 문자열로 변환 -> pgvector 쿼리 파라미터로 사용
		String embeddingString = Arrays.toString(embedding);
		
		Page<SearchResultProjection> diaryPage = searchRepository.findSimilarDiariesWithPagination(userId, embeddingString, pageable);
		
		return diaryPage.map(this::toSearchResultResponse);
	}
	
	private SearchResultResponse toSearchResultResponse(SearchResultProjection projection) {
		return new SearchResultResponse(
				projection.getId(), projection.getEntryDate(), projection.getTitle(),
				projection.getWeather(), projection.getContent(), projection.getCreatedAt()
		);
	}
}