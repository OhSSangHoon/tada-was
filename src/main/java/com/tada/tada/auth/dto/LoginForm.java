package com.tada.tada.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginForm {
	
	// 로그인 아이디
	@NotBlank(message = "아이디를 입력해주세요.")
	private String loginId;
	
	// 비밀번호
	@NotBlank(message = "비밀번호를 입력해주세요.")
	private String password;
}