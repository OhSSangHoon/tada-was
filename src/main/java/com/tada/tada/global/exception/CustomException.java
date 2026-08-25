package com.tada.tada.global.exception;

import lombok.Getter;


/*
	각 도메인에서 "에상 가능한 에러 상황"을 표현할 때 사용하는 예외.
	예: 일기를 못 찾음, 권한 없음, 이미 존재하는 아이디 등

	이 예외를 던지면 GlobalExceptionHandler가 잡아서
	ApiResponse.error(message) 형태로 자동 변환해 응답한다.
	→ 각 Controller/Service에서 try-catch를 매번 안 짜도 되게 해주는 역할.
*/

@Getter
public class CustomException extends RuntimeException {


	private final int statusCode; // HTTP status code (404, 400, 403 등)

	public CustomException(String message, int statusCode) {
		super(message); // 부모(RuntimeException)의 메시지로 등록 → getMessage()로 꺼낼 수 있음
		this.statusCode = statusCode;
	}
}