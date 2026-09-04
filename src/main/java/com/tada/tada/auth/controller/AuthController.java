package com.tada.tada.auth.controller;

import com.tada.tada.auth.dto.AuthResponse;
import com.tada.tada.auth.dto.LoginForm;
import com.tada.tada.auth.dto.RefreshTokenRequest;
import com.tada.tada.auth.dto.SignUpForm;
import com.tada.tada.auth.service.UsersService;
import com.tada.tada.global.exception.CustomException;
import com.tada.tada.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
	
	private final UsersService usersService;
	
	@PostMapping("/signup")
	public ApiResponse<Void> signUp(@Valid @RequestBody SignUpForm form) {
		usersService.signUp(form);
		return ApiResponse.success(null);
	}
	
	@PostMapping("/login")
	public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginForm form) {
		return ApiResponse.success(usersService.login(form));
	}
	
	// Access Token 인증 테스트
	@GetMapping("/me")
	public ApiResponse<UUID> me(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new CustomException("인증이 필요합니다.", 401);
		}
		
		UUID userId = (UUID) authentication.getPrincipal();
		return ApiResponse.success(userId);
	}
	
	// Refresh Token으로 Access Token 재발급
	@PostMapping("/reissue")
	public ApiResponse<AuthResponse> reissue(
			@Valid @RequestBody RefreshTokenRequest request
	) {
		return ApiResponse.success(
				usersService.reissue(request.getRefreshToken())
		);
	}
	
	// 로그아웃
	@PostMapping("/logout")
	public ApiResponse<Void> logout(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new CustomException("인증이 필요합니다.", 401);
		}
		
		UUID userId = (UUID) authentication.getPrincipal();
		usersService.logout(userId);
		return ApiResponse.success(null);
	}
}