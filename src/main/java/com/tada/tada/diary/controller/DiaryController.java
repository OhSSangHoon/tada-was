package com.tada.tada.diary.controller;

import com.tada.tada.diary.dto.DiaryCreateForm;
import com.tada.tada.diary.dto.DiaryResponse;
import com.tada.tada.diary.service.DiaryService;
import com.tada.tada.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
}
