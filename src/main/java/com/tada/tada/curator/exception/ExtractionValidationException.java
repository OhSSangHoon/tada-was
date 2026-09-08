package com.tada.tada.curator.exception;

import com.tada.tada.global.exception.CustomException;

import java.util.List;

/*
 * AI 가 돌려준 ExtractionResult 가 계약을 어겼을 때 던진다.
 *
 * 서버 버그가 아니라 외부 AI 응답 문제이므로 400 으로 응답한다.
 * 이 예외는 저장 트랜잭션 전체를 rollback 시키고,
 * 사용자에게는 "저장하지 못했다" 는 사실이 그대로 전달돼야 한다.
 *
 * 나머지 Curator 예외(IllegalArgument / IllegalState)는 내부 불변식 위반이라
 * 정상 흐름에서 발생하지 않는다. 그것들은 500 이 정직하므로 바꾸지 않는다.
 *
 * 사용자에게 보이는 메시지는 한국어 한 줄이고,
 * 어떤 검증이 깨졌는지는 getValidationErrors() 로 로그에만 남긴다.
 */
public class ExtractionValidationException
		extends CustomException {

	private static final int STATUS_CODE = 400;

	private static final String USER_MESSAGE =
			"AI 분석 결과가 올바르지 않아 일기를 저장하지 못했습니다.";

	private final List<String> validationErrors;

	public ExtractionValidationException(
			List<String> validationErrors
	) {
		super(
				USER_MESSAGE,
				STATUS_CODE
		);

		this.validationErrors =
				validationErrors == null
						? List.of()
						: List.copyOf(validationErrors);
	}

	public List<String> getValidationErrors() {
		return validationErrors;
	}

	/*
	 * 로그용 상세 문자열. 응답 본문에는 넣지 않는다.
	 */
	public String getDetail() {
		if (validationErrors.isEmpty()) {
			return "ExtractionResult validation failed";
		}

		return "ExtractionResult validation failed: "
				+ String.join(
				", ",
				validationErrors
		);
	}
}
