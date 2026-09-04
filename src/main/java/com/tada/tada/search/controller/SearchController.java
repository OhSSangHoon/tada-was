package com.tada.tada.search.controller;

import com.tada.tada.global.response.ApiResponse;
import com.tada.tada.search.dto.SearchResultResponse;
import com.tada.tada.search.service.SearchService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
	SearchController - RAG 검색 API 엔드포인트
	
	역할
	- 사용자의 자연어 검색 요청을 HTTP로 받아서
		SearchService에 전달하고, 결과를 공통 응답 포맷(ApiRespomse)으로 감싸서 응답
	- Controller는 Repository를 직접 호출하지 않고 반드시 Service를 거침
	- Entity(Diary)를 직접 반환하지 않고, Service가 변환해준 Response DTO만 다룸
	
 */
@RestController
@RequiredArgsConstructor	// final 필드 기반 생성자 주입
public class SearchController {
	
	// SearchService 주입 - 실제 검색 비지니스 로직 + entity -> DTO 변환
	private final SearchService searchService;
	
	/*
		자연어 검색어로 유사한 일기를 검색하는 API
		
		ex) POST /api/search?query=기분 나쁜날&page=0&size=3
		
		@param query 사용자가 입력한 자연어 검색어(필수)
			@RequestParam : URL 쿼리 파라미터(?query=...)에서 값을 꺼내옴
		@param page 조회할 페이지 번호 (기본값 0 = 첫 페이지)
		@param size 페이지당 결과 개수 (기본값 3)
		@return ApiResponse로 감싼 검색 결과 DTO Page 객체
			(Entity가 아니라 SearchResultResponse만 노출 - embedding 등 내부 필드 제외)
	 */
	@PostMapping("/api/search")
	public ApiResponse<Page<SearchResultResponse>> search(
			@RequestParam("query") String query,
			@RequestParam(value = "page", required = false, defaultValue = "0") int page,
			@RequestParam(value = "size", required = false, defaultValue = "3") int size
	){
		// page, size 값을 Pageable 객체로 변환
		Pageable pageable = PageRequest.of(page, size);
		
		// SearchService 호출 -> Page<SearchResultResponse> 반환받음
		Page<SearchResultResponse> result = searchService.search(query, pageable);
		
		// 공통 응답(ApiResponse)로 감싸서 반환
		return ApiResponse.success(result);
	}
}
