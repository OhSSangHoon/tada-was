package com.tada.tada.curator.controller;

import com.tada.tada.curator.dto.PersonCorrectionForm;
import com.tada.tada.curator.dto.PersonDetailResponse;
import com.tada.tada.curator.dto.PersonRenameForm;
import com.tada.tada.curator.dto.PersonSummaryResponse;
import com.tada.tada.curator.service.PersonCorrectionService;
import com.tada.tada.curator.service.PersonQueryService;
import com.tada.tada.curator.service.PersonRenameService;
import com.tada.tada.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/curator/persons")
@RequiredArgsConstructor
public class PersonController {

	private final PersonQueryService personQueryService;
	private final PersonCorrectionService personCorrectionService;
	private final PersonRenameService personRenameService;

	/*
	 * 검색 API 없음 — displayName + aliases 로 클라이언트가 필터한다.
	 */
	@GetMapping
	public ApiResponse<List<PersonSummaryResponse>> getAllPersons(
			Authentication authentication
	) {
		UUID userId = (UUID) authentication.getPrincipal();

		List<PersonSummaryResponse> response =
				personQueryService.getAllPersons(userId);

		return ApiResponse.success(response);
	}

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

	@PatchMapping("/{personId}/candidates/{candidateId}")
	public ApiResponse<Void> correctPerson(
			@PathVariable UUID personId,
			@PathVariable UUID candidateId,
			@RequestBody PersonCorrectionForm form,
			Authentication authentication
	) {
		UUID userId = (UUID) authentication.getPrincipal();

		personCorrectionService.correctPerson(
				userId,
				personId,
				candidateId,
				form
		);

		return ApiResponse.success(null);
	}

	@PatchMapping("/{personId}")
	public ApiResponse<Void> renamePerson(
			@PathVariable UUID personId,
			@RequestBody PersonRenameForm form,
			Authentication authentication
	) {
		UUID userId = (UUID) authentication.getPrincipal();

		personRenameService.renamePerson(
				userId,
				personId,
				form
		);

		return ApiResponse.success(null);
	}
}