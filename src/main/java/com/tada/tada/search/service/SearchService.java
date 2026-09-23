package com.tada.tada.search.service;

import com.tada.tada.global.exception.CustomException;
import com.tada.tada.search.dto.SearchResultProjection;
import com.tada.tada.search.dto.SearchResultResponse;
import com.tada.tada.search.dto.SearchSortOption;
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
    - 임베딩 생성(Voyage AI 호출) 실패 시 503 (외부 서비스 장애로 간주)
      단, Voyage 429(rate limit)는 짧게 대기 후 1회 재시도 (embedWithRetry)
 */

@Service
@RequiredArgsConstructor
public class SearchService {
	
	// SearchRepository 주입 - pgvector 검색 쿼리 실행 담당
	private final SearchRepository searchRepository;
	
	// VoyageAIEmbeddingService 주입 - 텍스트 -> 벡터 변환 담당
	private final VoyageAIEmbeddingService voyageAIEmbeddingService;
	
	// 코사인 거리(embedding <-> embedding) 임계값 - 이보다 작아야 "관련 있는" 일기로 간주
	private static final double SIMILARITY_THRESHOLD = 0.9;
	
	// Voyage AI 429(rate limit) 시 재시도 관련 설정
	private static final int SEARCH_EMBED_MAX_RETRIES = 2;
	private static final long SEARCH_EMBED_RETRY_WAIT_MS = 2000; // 2초
	
	/*
	   자연어 검색으로 유사한 일기 페이지네이션과 함께 검색
	   
	   동작 순서
	   - query 공백 여부 검증
	   - queryText를 임베딩 벡터로 변환 (실패 시 CustomException)
	   - float[] 벡터를 pgvector가 이해하는 문자열 형태로 변환
	   - SearchRepository의 네이티브 쿼리로 본인 일기 중 코사인 거리 기반 검색
		 (SIMILARITY_THRESHOLD보다 가까운, 즉 쿼리와 관련 있는 일기만 후보로 필터링)
	   - 결과를 Page 객체로 반환
	   
	   @param userId 검색을 요청한 로그인 사용자 ID
	   @param queryText 사용자가 입력한 자연어 검색어
	   @param pageable 페이지 정보
	   @return 유사도순으로 정렬된 본인 일기 Page 객체
	 */
	public Page<SearchResultResponse> search(UUID userId, String queryText, Pageable pageable, SearchSortOption sort) {
		
		// 검색어가 비어있으면 임베딩 계산 (외부 API 호출)까지 갈 필요 없이 바로 막음
		if (queryText == null || queryText.isBlank()) {
			throw new CustomException("검색어를 입력해주세요.", 400);
		}
		
		// 쿼리 텍스트를 벡터로 변환 (1024차원 float 배열)
		float[] embedding = embedWithRetry(queryText);
		
		// float[] 배열을 문자열로 변환 -> pgvector 쿼리 파라미터로 사용
		String embeddingString = Arrays.toString(embedding);
		
		Page<SearchResultProjection> diaryPage = switch (sort) {
			case LATEST -> searchRepository.findSimilarDiariesOrderByEntryDateDesc(
					userId, embeddingString, SIMILARITY_THRESHOLD, pageable);
			case OLDEST -> searchRepository.findSimilarDiariesOrderByEntryDateAsc(
					userId, embeddingString, SIMILARITY_THRESHOLD, pageable);
		};
		
		return diaryPage.map(this::toSearchResultResponse);
	}
	
	/*
	   검색어 임베딩 생성 - Voyage AI 429(rate limit) 발생 시 짧게 대기 후 1회 재시도
	   429가 아닌 다른 예외는 재시도 없이 바로 실패 처리
	 */
	private float[] embedWithRetry(String queryText) {
		for (int attempt = 1; attempt <= SEARCH_EMBED_MAX_RETRIES; attempt++) {
			try {
				return voyageAIEmbeddingService.embed(queryText);
			} catch (CustomException e) {
				boolean isRateLimit = e.getStatusCode() == 429;
				boolean hasMoreAttempts = attempt < SEARCH_EMBED_MAX_RETRIES;
				
				if (isRateLimit && hasMoreAttempts) {
					try {
						Thread.sleep(SEARCH_EMBED_RETRY_WAIT_MS);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
					continue;
				}
				break;
			} catch (Exception e) {
				break;
			}
		}
		throw new CustomException("검색어 임베딩 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.", 503);
	}
	
	private SearchResultResponse toSearchResultResponse(
			SearchResultProjection projection
	) {
		return new SearchResultResponse(
				projection.getId(), projection.getEntryDate(), projection.getTitle(),
				projection.getWeather(), projection.getContent(), projection.getCreatedAt(),
				projection.getStickerImageUrl()       // LEFT JOIN이라 스티커 없으면 자동으로 null
		);
	}
	
	
}