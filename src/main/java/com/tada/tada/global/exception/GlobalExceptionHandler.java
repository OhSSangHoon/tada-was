package com.tada.tada.global.exception;

/*
* 프로젝트 전체에서 발생하는 예외를 한 곳에서 잡아서
* ApiResponse.error(...) 형태로 통일해 응답하는 클래스.
*
* 각 Controller마다 try-catch를 반복해서 짤 필요 없이,
* 예외를 그냥 던지기만 하면(throw) 여기서 자동으로 잡아 처리해준다.
*
* @RestControllerAdvice: 모든 @RestController에서 발생하는 예외를
* 감시하겠다는 뜻 (프로젝트 전역에 적용)
* */

import com.tada.tada.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
	/*
	* 존재하지 않는 경로("/"나 오타난 URL 등)로 요청이 왔을 때 Spring이 던지는 예외.
	* 아래 catch-all(Exception.class)이 이것까지 잡아서 500으로 응답해버리는 걸 막기 위해
	* 먼저 잡아서 정직하게 404로 응답한다.
	* */
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("요청하신 경로를 찾을 수 없습니다."));
	}
	/*
	* 우리가 직접 만든 CustomException을 처리.
	* throw new CustomException ("일기를 찾을 수 없습니다", 404);
	* → 이 메서드가 잡아서 { success: false, message: "일기를 찾을 수 없습니다" } 로 응답
	*
	*  */
	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
		return ResponseEntity
				.status(e.getStatusCode())
				.body(ApiResponse.error(e.getMessage()));
	}

	/*
	* @Valid 붙은 Request DTO의 검증(@NotEmpty 등)이 실패했을 때 자동으로 발생하는 예외.
	* CreateDiaryRequest의 content가 비어있는 채로 요청이 오면 여기서 잡힘.
	* 여러 필드가 동시에 틀릴 수 있어서, 첫 번째 에러 메시지만 꺼내서 응답한다.
	* */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error(message));
	}

	/*
	* 위에서 처리 안 된, 예상 못한 모든 에러를 마지막에 잡는 안전망
	* null 참조, 배열 범위 초과 같은 진짜 버그성 에러
	* 이게 없으면 이런 에러가 났을 때 서버가 그대로 500 에러 페이지(HTML)를 뱉어서
	* 프론트가 JSON 파싱 실패로 또 다른 에러를 겪게 된다.
	* */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleEception(Exception e){
		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("서버 내부 오류가 발생했습니다."));
	}
}

