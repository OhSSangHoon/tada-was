package com.tada.tada.curator.controller;

import com.tada.tada.curator.dto.PersonDetailResponse;
import com.tada.tada.curator.dto.PersonSummaryResponse;
import com.tada.tada.curator.service.PersonQueryService;
import com.tada.tada.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {

	private final PersonQueryService personQueryService;

	/*
	 * 홈 사람 목록.
	 * 검색 API 는 두지 않는다. 이 응답의 displayName + aliases 로 클라이언트가 필터한다.
	 */
	@GetMapping
	public ApiResponse<List<PersonSummaryResponse>> getPersonList(
			Authentication authentication
	) {
		UUID userId = (UUID) authentication.getPrincipal();

		List<PersonSummaryResponse> response =
				personQueryService.getPersonList(userId);

		return ApiResponse.success(response);
	}

	/*
	 * 사람 상세의 헤더 + 통계 + 한눈에 보기.
	 * 없는 사람과 남의 사람은 모두 404 다.
	 */
	@GetMapping("/{id}")
	public ApiResponse<PersonDetailResponse> getPersonDetail(
			@PathVariable UUID id,
			Authentication authentication
	) {
		UUID userId = (UUID) authentication.getPrincipal();

		PersonDetailResponse response =
				personQueryService.getPersonDetail(userId, id);

		return ApiResponse.success(response);
	}
}
