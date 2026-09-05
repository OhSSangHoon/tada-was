package com.tada.tada.curator.validation;

import com.tada.tada.curator.exception.ExtractionValidationException;
import com.tada.tada.curator.service.PersonNormalizer;
import com.tada.tada.global.event.dto.ExtractionResult;
import com.tada.tada.global.event.dto.PersonExtraction;
import com.tada.tada.global.event.dto.PlaceExtraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtractionResultValidatorTest {

	private ExtractionResultValidator validator;

	@BeforeEach
	void setUp() {
		validator =
				new ExtractionResultValidator(
						new PersonNormalizer()
				);
	}

	@Test
	void PERSON_ref는_비어있지_않고_유일하면_형식과_무관하게_허용한다() {
		ExtractionResult result =
				new ExtractionResult(
						List.of(
								new PersonExtraction(
										"person-minsu",
										"민수",
										"PERSON"
								)
						),
						List.of(
								new PlaceExtraction(
										"카페",
										"카페",
										"PLACE",
										List.of(
												"person-minsu"
										)
								)
						),
						List.of()
				);

		assertDoesNotThrow(
				() -> validator.validate(
						"민수와 카페에 갔다",
						result
				)
		);
	}

	@Test
	void 중복된_PERSON_ref는_거부한다() {
		ExtractionResult result =
				new ExtractionResult(
						List.of(
								new PersonExtraction(
										"person-1",
										"민수",
										"PERSON"
								),
								new PersonExtraction(
										"person-1",
										"철수",
										"PERSON"
								)
						),
						List.of(),
						List.of()
				);

		ExtractionValidationException exception =
				assertThrows(
						ExtractionValidationException.class,
						() -> validator.validate(
								"민수와 철수를 만났다",
								result
						)
				);

		assertTrue(
				exception.getValidationErrors()
						.stream()
						.anyMatch(
								error ->
										error.contains(
												"duplicated"
										)
						)
		);
	}

	@Test
	void 존재하지_않는_PERSON_ref를_참조하면_거부한다() {
		ExtractionResult result =
				new ExtractionResult(
						List.of(
								new PersonExtraction(
										"person-minsu",
										"민수",
										"PERSON"
								)
						),
						List.of(
								new PlaceExtraction(
										"카페",
										"카페",
										"PLACE",
										List.of(
												"person-chulsu"
										)
								)
						),
						List.of()
				);

		ExtractionValidationException exception =
				assertThrows(
						ExtractionValidationException.class,
						() -> validator.validate(
								"민수와 카페에 갔다",
								result
						)
				);

		assertTrue(
				exception.getValidationErrors()
						.stream()
						.anyMatch(
								error ->
										error.contains(
												"unknown PERSON ref"
										)
						)
		);
	}

	@Test
	void rawText가_일기_원문에_없으면_거부한다() {
		ExtractionResult result =
				new ExtractionResult(
						List.of(
								new PersonExtraction(
										"person-minsu",
										"철수",
										"PERSON"
								)
						),
						List.of(),
						List.of()
				);

		ExtractionValidationException exception =
				assertThrows(
						ExtractionValidationException.class,
						() -> validator.validate(
								"민수를 만났다",
								result
						)
				);

		assertTrue(
				exception.getValidationErrors()
						.stream()
						.anyMatch(
								error ->
										error.contains(
												"exact diary substring"
										)
						)
		);
	}

	@Test
	void 대명사와_불특정_지칭은_인물로_저장하지_않는다() {
		/*
		 * 명세 8.2-6: 빈 문자열, 대명사, 일반 명사만 남으면
		 * 정상 PERSON 으로 저장하지 않고 Extraction 검증 실패로 처리한다.
		 */
		for (String rawText : java.util.List.of(
				"그", "그녀", "걔", "쟤", "얘",
				"우리", "저희", "누구", "아무",
				"사람들", "다들", "모두", "애들", "친구들",
				"그는", "걔가", "사람들이", "모두가"
		)) {
			assertThrows(
					ExtractionValidationException.class,
					() -> validate(rawText),
					rawText
			);
		}
	}

	@Test
	void 정규화_결과가_비면_인물로_저장하지_않는다() {
		for (String rawText : java.util.List.of("...", "!!!", "ㅁㅅ")) {
			assertThrows(
					ExtractionValidationException.class,
					() -> validate(rawText),
					rawText
			);
		}
	}

	@Test
	void 실제_인물_지칭은_그대로_통과한다() {
		for (String rawText : java.util.List.of(
				"민수", "민수가", "김민혁", "김성은", "가을이",
				"엄마", "아빠", "형", "형이랑", "동생", "친구",
				"선생님", "제임스", "쌤이랑"
		)) {
			assertDoesNotThrow(
					() -> validate(rawText),
					rawText
			);
		}
	}

	private void validate(String personRawText) {
		validator.validate(
				"오늘 " + personRawText + " 만났다",
				new ExtractionResult(
						java.util.List.of(
								new PersonExtraction(
										"p1",
										personRawText,
										"PERSON"
								)
						),
						java.util.List.of(),
						java.util.List.of()
				)
		);
	}

	/*
	 * AI 응답 계약 위반은 서버 버그가 아니라 외부 입력 문제다.
	 * 500 이 아니라 400 으로 나가고, 사용자에게는 한국어 한 줄이 보여야 한다.
	 * 어떤 검증이 깨졌는지는 로그로만 남긴다.
	 */
	@Test
	void 검증_실패는_400과_사용자_메시지를_가진다() {
		ExtractionValidationException exception =
				assertThrows(
						ExtractionValidationException.class,
						() -> validator.validate(
								"오늘 민수와 놀았다",
								new ExtractionResult(
										java.util.List.of(
												new PersonExtraction(
														"p1",
														"없는이름",
														"PERSON"
												)
										),
										java.util.List.of(),
										java.util.List.of()
								)
						)
				);

		org.junit.jupiter.api.Assertions.assertEquals(
				400,
				exception.getStatusCode()
		);

		org.junit.jupiter.api.Assertions.assertEquals(
				"AI 분석 결과가 올바르지 않아 일기를 저장하지 못했습니다.",
				exception.getMessage()
		);

		org.junit.jupiter.api.Assertions.assertFalse(
				exception.getValidationErrors().isEmpty()
		);

		org.junit.jupiter.api.Assertions.assertTrue(
				exception.getDetail()
						.contains("ExtractionResult validation failed")
		);
	}
}
