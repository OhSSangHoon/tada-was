package com.tada.tada.auth.service;

import com.tada.tada.auth.dto.AuthResponse;
import com.tada.tada.auth.dto.LoginForm;
import com.tada.tada.auth.dto.SignUpForm;
import com.tada.tada.auth.entity.Users;
import com.tada.tada.auth.repository.UsersRepository;
import com.tada.tada.global.exception.CustomException;
import com.tada.tada.global.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UsersService {
	
	private final UsersRepository usersRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	
	// 일반 회원가입
	public void signUp(SignUpForm form) {
		
		// 비밀번호 확인
		if (!form.getPassword().equals(form.getPasswordConfirm())) {
			throw new CustomException("비밀번호가 일치하지 않습니다.", 400);
		}
		
		// 아이디 중복 확인
		if (usersRepository
				.findByLoginIdAndProvider(form.getLoginId(), "local")
				.isPresent()) {
			throw new CustomException("이미 사용 중인 아이디입니다.", 400);
		}
		
		// 비밀번호 암호화
		String encodedPassword =
				passwordEncoder.encode(form.getPassword());
		
		// 회원 생성
		Users users = new Users(
				form.getLoginId(),
				encodedPassword,
				form.getNickname()
		);
		
		// DB에 회원 저장
		usersRepository.save(users);
	}
	
	// 로그인
	public AuthResponse login(LoginForm form) {
		
		// 회원 조회
		Users users = usersRepository
				.findByLoginIdAndProvider(form.getLoginId(), "local")
				.orElseThrow(() ->
						new CustomException(
								"아이디 또는 비밀번호가 일치하지 않습니다.", 401));
		
		// 비밀번호 확인
		if (!passwordEncoder.matches(form.getPassword(), users.getPassword())) {
			throw new CustomException(
					"아이디 또는 비밀번호가 일치하지 않습니다.", 401);
		}
		
		// JWT 발급
		String accessToken = jwtUtil.createToken(users.getId());
		
		return new AuthResponse(accessToken);
	}
}