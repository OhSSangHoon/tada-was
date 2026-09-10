package com.tada.tada.diary.controller;

import com.tada.tada.diary.dto.CanCreateResponse;
import com.tada.tada.diary.dto.DiaryCreateForm;
import com.tada.tada.diary.dto.DiaryResponse;
import com.tada.tada.diary.dto.DiaryUpdateForm;
import com.tada.tada.diary.service.DiaryService;
import com.tada.tada.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {
	private final DiaryService diaryService;
	
	@PostMapping
	public ApiResponse<DiaryResponse> createDiary(
			@RequestBody @Valid DiaryCreateForm form,
			Authentication authentication
			) {
		UUID userId = (UUID) authentication.getPrincipal();
		
		DiaryResponse response = diaryService.createDiary(userId, form);
		return ApiResponse.success(response);
	}
	
	@GetMapping("/{id}")
	public ApiResponse<DiaryResponse> getDiary(
			@PathVariable UUID id,
			Authentication authentication
	) {
		UUID userId = (UUID) authentication.getPrincipal();
		
		DiaryResponse response = diaryService.getDiary(userId, id);
		return ApiResponse.success(response);
	}
	
	@PutMapping("/{id}")
	public ApiResponse<DiaryResponse> updateDiary(
			@PathVariable UUID id,
			@RequestBody @Valid DiaryUpdateForm form,
			Authentication authentication
			) {
		UUID userId = (UUID) authentication.getPrincipal();
		
		DiaryResponse response = diaryService.updateDiary(userId, id, form);
		return ApiResponse.success(response);
	}
	
	@DeleteMapping("/{id}")
	public ApiResponse<Void> trashDiary(
			@PathVariable UUID id,
			Authentication authentication
			) {
		UUID userId = (UUID) authentication.getPrincipal();
		
		diaryService.trashDiary(userId, id);
		return ApiResponse.success(null);
	}
	
	@PostMapping("/{id}/restore")
	public ApiResponse<DiaryResponse> restoreDiary(
			@PathVariable UUID id,
			@RequestParam(defaultValue = "false") boolean replace,
			Authentication authentication
			) {
		UUID userId = (UUID) authentication.getPrincipal();
		
		DiaryResponse response = diaryService.restoreDiary(userId, id, replace);
		return ApiResponse.success(response);
	}
	
	@GetMapping("/can-create")
	public ApiResponse<CanCreateResponse> canCreate(
			@RequestParam LocalDate date,
			Authentication authentication
			) {
		UUID userId = (UUID) authentication.getPrincipal();
		
		CanCreateResponse response = diaryService.canCreate(userId, date);
		return ApiResponse.success(response);
	}
	
	@GetMapping("/trash")
	public ApiResponse<List<DiaryResponse>> getAllTrashedDiaries(Authentication authentication) {
		UUID userId = (UUID) authentication.getPrincipal();
		
		List<DiaryResponse> responses = diaryService.getAllTrashedDiaries(userId);
		return ApiResponse.success(responses);
	}
}
