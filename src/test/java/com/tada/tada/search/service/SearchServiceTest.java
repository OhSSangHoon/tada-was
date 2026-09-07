package com.tada.tada.search.service;

import com.tada.tada.global.exception.CustomException;
import com.tada.tada.search.dto.SearchResultProjection;
import com.tada.tada.search.dto.SearchResultResponse;
import com.tada.tada.search.repository.SearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchServiceTest {
	
	private SearchRepository searchRepository;
	private VoyageAIEmbeddingService voyageAIEmbeddingService;
	private SearchService searchService;
	
	@BeforeEach
	void setUp() {
		searchRepository = Mockito.mock(SearchRepository.class);
		voyageAIEmbeddingService = Mockito.mock(VoyageAIEmbeddingService.class);
		
		searchService = new SearchService(
				searchRepository,
				voyageAIEmbeddingService
		);
	}
	
	@Test
	void 검색어가_빈문자열이면_예외를_던진다() {
		UUID userId = UUID.randomUUID();
		Pageable pageable = Pageable.ofSize(3);
		
		CustomException exception = assertThrows(
				CustomException.class,
				() -> searchService.search(userId, "   ", pageable)
		);
		
		assertEquals("검색어를 입력해주세요.", exception.getMessage());
		assertEquals(400, exception.getStatusCode());
	}
	
	@Test
	void 검색어가_null이면_예외를_던진다() {
		UUID userId = UUID.randomUUID();
		Pageable pageable = Pageable.ofSize(3);
		
		CustomException exception = assertThrows(
				CustomException.class,
				() -> searchService.search(userId, null, pageable)
		);
		
		assertEquals("검색어를 입력해주세요.", exception.getMessage());
		assertEquals(400, exception.getStatusCode());
	}
	
	@Test
	void 임베딩_생성에_실패하면_예외를_던진다() {
		UUID userId = UUID.randomUUID();
		Pageable pageable = Pageable.ofSize(3);
		
		when(voyageAIEmbeddingService.embed(anyString()))
				.thenThrow(new RuntimeException("Voyage AI 호출 실패"));
		
		CustomException exception = assertThrows(
				CustomException.class,
				() -> searchService.search(userId, "기분 좋은 날", pageable)
		);
		
		assertEquals("검색어 임베딩 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.", exception.getMessage());
		assertEquals(503, exception.getStatusCode());
	}
	
	@Test
	void 정상_검색이면_본인_userId로_조회하고_결과를_DTO로_변환한다() {
		UUID userId = UUID.randomUUID();
		Pageable pageable = Pageable.ofSize(3);
		
		float[] dummyEmbedding = new float[]{0.1f, 0.2f, 0.3f};
		when(voyageAIEmbeddingService.embed("기분 좋은 날"))
				.thenReturn(dummyEmbedding);
		
		UUID diaryId = UUID.randomUUID();
		SearchResultProjection projection = createProjection(
				diaryId,
				LocalDate.of(2026, 8, 1),
				"좋은 하루",
				"맑음",
				"오늘은 기분이 좋았다",
				LocalDateTime.of(2026, 8, 1, 21, 0)
		);
		
		Page<SearchResultProjection> projectionPage =
				new PageImpl<>(List.of(projection), pageable, 1);
		
		when(searchRepository.findSimilarDiariesWithPagination(
				eq(userId),
				anyString(),
				eq(pageable)
		)).thenReturn(projectionPage);
		
		Page<SearchResultResponse> result =
				searchService.search(userId, "기분 좋은 날", pageable);
		
		assertEquals(1, result.getContent().size());
		assertEquals(diaryId, result.getContent().get(0).getId());
		assertEquals("좋은 하루", result.getContent().get(0).getTitle());
		
		// 본인(userId) 기준으로 필터링해서 조회했는지 검증 (다른 사용자 일기 노출 방지)
		verify(searchRepository).findSimilarDiariesWithPagination(
				eq(userId),
				anyString(),
				eq(pageable)
		);
	}
	
	private SearchResultProjection createProjection(
			UUID id,
			LocalDate entryDate,
			String title,
			String weather,
			String content,
			LocalDateTime createdAt
	) {
		SearchResultProjection projection = Mockito.mock(SearchResultProjection.class);
		
		when(projection.getId()).thenReturn(id);
		when(projection.getEntryDate()).thenReturn(entryDate);
		when(projection.getTitle()).thenReturn(title);
		when(projection.getWeather()).thenReturn(weather);
		when(projection.getContent()).thenReturn(content);
		when(projection.getCreatedAt()).thenReturn(createdAt);
		
		return projection;
	}
}