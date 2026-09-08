package com.tada.tada.search.controller;

import com.tada.tada.global.exception.CustomException;
import com.tada.tada.global.response.ApiResponse;
import com.tada.tada.search.dto.SearchResultResponse;
import com.tada.tada.search.service.SearchService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/*
    SearchController - RAG 검색 API 엔드포인트
    
    역할
    - 사용자의 자연어 검색 요청을 HTTP로 받아서
       SearchService에 전달하고, 결과를 공통 응답 포맷(ApiResponse)으로 감싸서 응답
    - Controller는 Repository를 직접 호출하지 않고 반드시 Service를 거침
    - Entity(Diary)를 직접 반환하지 않고, Service가 변환해준 Response DTO만 다룸
    - 단순 조회이고 body가 필요 없어서 GET + 쿼리파라미터로 구현 (코드리뷰 반영)
    - Authentication에서 userId를 꺼내 본인 일기만 검색되도록 제한 (코드리뷰 반영)
 */
@RestController
@RequiredArgsConstructor    // final 필드 기반 생성자 주입
public class SearchController {
	
	// 페이지네이션 기본값 - 매직 넘버 금지 컨벤션에 따라 상수로 분리
	private static final String DEFAULT_PAGE = "0";
	private static final String DEFAULT_PAGE_SIZE = "3";
	
	// SearchService 주입 - 실제 검색 비지니스 로직 + entity -> DTO 변환
	private final SearchService searchService;
	
	/*
	   자연어 검색어로 유사한 일기를 검색하는 API
	   
	   ex) GET /api/search?query=기분 나쁜날&page=0&size=3
	   
	   @param query 사용자가 입력한 자연어 검색어(필수)
	   @RequestParam : URL 쿼리 파라미터(?query=...)에서 값을 꺼내옴
	   @param page 조회할 페이지 번호 (기본값 0 = 첫 페이지, 음수 불가)
	   @param size 페이지당 결과 개수 (기본값 3, 1 이상)
	   @param authentication JwtAuthFilter가 채워준 인증 정보 (principal = 로그인 사용자 UUID)
	   @return ApiResponse로 감싼 검색 결과 DTO Page 객체
		  (Entity가 아니라 SearchResultResponse만 노출 - embedding 등 내부 필드 제외)
		  (본인이 작성한 일기만 검색 대상 - 다른 사용자 일기는 제외)
	 */
	@GetMapping("/api/search")
	public ApiResponse<Page<SearchResultResponse>> search(
			@RequestParam("query") String query,
			@RequestParam(value = "page", required = false, defaultValue = DEFAULT_PAGE) int page,
			@RequestParam(value = "size", required = false, defaultValue = DEFAULT_PAGE_SIZE) int size,
			Authentication authentication
	){
		// JwtAuthFilter가 SecurityContext에 넣어둔 인증 정보에서 로그인 사용자 ID 추출
		UUID userId = (UUID) authentication.getPrincipal();
		
		// page/size는 API 입력 파라미터 검증이라 Controller에서 처리
		// (PageRequest.of()가 그대로 던지는 IllegalArgumentException은 GlobalExceptionHandler의 catch-all에 걸려 500으로 나감, 먼저 걸러서 400으로 응답)
		if (page < 0) {
			throw new CustomException("page는 0 이상이어야 합니다.", 400);
		}
		if (size < 1) {
			throw new CustomException("size는 1 이상이어야 합니다.", 400);
		}
		
		// page, size 값을 Pageable 객체로 변환
		Pageable pageable = PageRequest.of(page, size);
		
		// SearchService 호출 -> Page<SearchResultResponse> 반환받음 (본인 일기로 제한)
		Page<SearchResultResponse> result = searchService.search(userId, query, pageable);
		
		// 공통 응답(ApiResponse)로 감싸서 반환
		return ApiResponse.success(result);
	}
}