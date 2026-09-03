package com.tada.tada.curator.validation;

import com.tada.tada.curator.exception.ExtractionValidationException;
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
				new ExtractionResultValidator();
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
}