package com.tada.tada.auth.controller;

import com.tada.tada.auth.dto.AuthResponse;
import com.tada.tada.auth.dto.LoginForm;
import com.tada.tada.auth.dto.SignUpForm;
import com.tada.tada.auth.service.UsersService;
import com.tada.tada.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
	
	private final UsersService usersService;
	
	// 회원가입 요청을 Service로 전달
	@PostMapping("/signup")
	public ApiResponse<Void> signUp(@Valid @RequestBody SignUpForm form) {
		usersService.signUp(form);
		return ApiResponse.success(null);
	}
	
	// 로그인 요청을 Service로 전달
	@PostMapping("/login")
	public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginForm form) {
		return ApiResponse.success(usersService.login(form));
	}
}