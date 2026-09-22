package com.tada.tada.curator.exception;

import com.tada.tada.global.exception.CustomException;

import java.util.List;

/*
 * AI ExtractionResult 계약 위반 시 던진다. 서버 버그가 아닌 외부 응답 문제라 400으로 응답하고
 * 저장 트랜잭션 전체를 rollback한다. 다른 Curator 예외(IllegalArgument/IllegalState)는 내부
 * 불변식 위반이라 500이 정직하므로 바꾸지 않는다.
 * 사용자 메시지는 한국어 한 줄, 상세 검증 실패는 getValidationErrors()로 로그에만 남긴다.
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
	 * 로그용 상세 문자열 (응답 본문에는 포함하지 않는다).
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
