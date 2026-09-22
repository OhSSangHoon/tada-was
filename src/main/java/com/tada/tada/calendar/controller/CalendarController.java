package com.tada.tada.calendar.controller;

import com.tada.tada.calendar.dto.CalendarResponse;
import com.tada.tada.calendar.service.CalendarService;
import com.tada.tada.global.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
@Validated
public class CalendarController {
	
	private final CalendarService calendarService;
	
	@GetMapping
	public ApiResponse<List<CalendarResponse>> getCalendar(
			@RequestParam int year,
			@RequestParam @Min(1) @Max(12) int month,
			Authentication authentication
			) {
		UUID userId = (UUID) authentication.getPrincipal();
		
		List<CalendarResponse> response = calendarService.getCalendar(userId, year, month);
		return ApiResponse.success(response);
	}
}
