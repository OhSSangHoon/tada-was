package com.tada.tada.search.service;

import com.tada.tada.search.dto.SearchResultProjection;
import com.tada.tada.search.dto.SearchResultResponse;
import com.tada.tada.search.repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchService {
	
	private final SearchRepository searchRepository;
	private final VoyageAIEmbeddingService voyageAIEmbeddingService;
	
	public Page<SearchResultResponse> search(UUID userId, String queryText, Pageable pageable) {
		
		float[] embedding = voyageAIEmbeddingService.embed(queryText);
		
		String embeddingString = Arrays.toString(embedding);
		
		Page<SearchResultProjection> diaryPage = searchRepository.findSimilarDiariesWithPagination(userId, embeddingString, pageable);
		
		return diaryPage.map(this::toSearchResultResponse);
	}
	private SearchResultResponse toSearchResultResponse(SearchResultProjection  projection) {
		return new SearchResultResponse(
				projection.getId(), projection.getEntryDate(), projection.getTitle(),
				projection.getWeather(), projection.getContent(), projection.getCreatedAt()
		);
	}
}