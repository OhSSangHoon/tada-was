package com.tada.tada.curator.controller;

import com.tada.tada.curator.dto.MemoryRecallResponse;
import com.tada.tada.curator.service.MemoryRecallService;
import com.tada.tada.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/curator/memory-recalls")
@RequiredArgsConstructor
public class MemoryRecallController {

	private final MemoryRecallService memoryRecallService;

	@GetMapping
	public ApiResponse<MemoryRecallResponse> getMemoryRecall(
			Authentication authentication
	) {
		UUID userId =
				(UUID) authentication
						.getPrincipal();

		MemoryRecallResponse response =
				memoryRecallService
						.getMemoryRecall(
								userId
						);

		return ApiResponse.success(response);
	}
}