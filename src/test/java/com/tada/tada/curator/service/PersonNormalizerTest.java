package com.tada.tada.curator.service;

import com.tada.tada.curator.model.PersonNormalization;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonNormalizerTest {

	private final PersonNormalizer personNormalizer =
			new PersonNormalizer();

	@Test
	void 받침이_없는_이름의_안전한_조사를_제거한다() {
		PersonNormalization result =
				personNormalizer.normalize("민수와");

		assertEquals("민수", result.normalizedText());
		assertEquals(
				List.of("민수와", "민수"),
				result.strongMatchCandidates()
		);
		assertEquals(
				List.of(),
				result.weakMatchCandidates()
		);
	}

	@Test
	void 이름_뒤의_이와를_강하게_정규화한다() {
		PersonNormalization result =
				personNormalizer.normalize("한영이와");

		assertEquals("한영", result.normalizedText());
		assertEquals(
				List.of("한영이와", "한영"),
				result.strongMatchCandidates()
		);
	}

	@Test
	void 이름_뒤의_이랑을_강하게_정규화한다() {
		PersonNormalization result =
				personNormalizer.normalize("지훈이랑");

		assertEquals("지훈", result.normalizedText());
		assertEquals(
				List.of("지훈이랑", "지훈"),
				result.strongMatchCandidates()
		);
	}

	@Test
	void 이름_뒤의_이가를_강하게_정규화한다() {
		PersonNormalization result =
				personNormalizer.normalize("한영이가");

		assertEquals("한영", result.normalizedText());
		assertEquals(
				List.of("한영이가", "한영"),
				result.strongMatchCandidates()
		);
	}

	@Test
	void 단일_은은_강하게_제거하지_않는다() {
		PersonNormalization result =
				personNormalizer.normalize("한영은");

		assertEquals(
				"한영은",
				result.normalizedText()
		);

		assertEquals(
				List.of("한영은"),
				result.strongMatchCandidates()
		);

		assertEquals(
				List.of("한영"),
				result.weakMatchCandidates()
		);
	}

	@Test
	void 단일_는은_강하게_제거하지_않는다() {
		PersonNormalization result =
				personNormalizer.normalize("민수는");

		assertEquals(
				"민수는",
				result.normalizedText()
		);

		assertEquals(
				List.of("민수는"),
				result.strongMatchCandidates()
		);

		assertEquals(
				List.of("민수"),
				result.weakMatchCandidates()
		);
	}

	@Test
	void 단일_이는_강하게_제거하지_않는다() {
		PersonNormalization result =
				personNormalizer.normalize("민혁이");

		assertEquals(
				"민혁이",
				result.normalizedText()
		);

		assertEquals(
				List.of("민혁이"),
				result.strongMatchCandidates()
		);

		assertEquals(
				List.of("민혁"),
				result.weakMatchCandidates()
		);
	}

	@Test
	void 실제_이름의_은을_조사로_오인하지_않는다() {
		PersonNormalization result =
				personNormalizer.normalize("박지은");

		assertEquals(
				"박지은",
				result.normalizedText()
		);

		assertEquals(
				List.of("박지은"),
				result.strongMatchCandidates()
		);

		assertEquals(
				List.of(),
				result.weakMatchCandidates()
		);
	}

	@Test
	void 호칭_제거는_약한_후보로만_사용한다() {
		PersonNormalization result =
				personNormalizer.normalize("민수씨");

		assertEquals(
				"민수씨",
				result.normalizedText()
		);

		assertEquals(
				List.of("민수씨"),
				result.strongMatchCandidates()
		);

		assertEquals(
				List.of("민수"),
				result.weakMatchCandidates()
		);
	}

	@Test
	void 조사와_호칭이_함께_있어도_약한_후보를_단계적으로_만든다() {
		PersonNormalization result =
				personNormalizer.normalize("민수씨는");

		assertEquals(
				"민수씨는",
				result.normalizedText()
		);

		assertEquals(
				List.of("민수씨"),
				result.weakMatchCandidates()
						.subList(0, 1)
		);

		assertEquals(
				List.of("민수씨", "민수"),
				result.weakMatchCandidates()
		);
	}

	@Test
	void 긴_조사를_안전하게_제거한다() {
		assertEquals(
				"영희",
				personNormalizer
						.normalize("영희에게")
						.normalizedText()
		);

		assertEquals(
				"영희",
				personNormalizer
						.normalize("영희에게서")
						.normalizedText()
		);
	}

	@Test
	void 도를_안전하게_제거한다() {
		assertEquals(
				"민수",
				personNormalizer
						.normalize("민수도")
						.normalizedText()
		);
	}

	@Test
	void 기존_호환_메서드는_강한_후보만_반환한다() {
		assertEquals(
				List.of("한영이와", "한영"),
				personNormalizer.normalizeCandidates(
						"한영이와"
				)
		);

		assertEquals(
				List.of("한영은"),
				personNormalizer.normalizeCandidates(
						"한영은"
				)
		);
	}

	@Test
	void 공백과_구두점을_정리한다() {
		assertEquals(
				"민수",
				personNormalizer
						.normalize("  민수와!!!  ")
						.normalizedText()
		);
	}

	@Test
	void 빈_값은_빈_정규화_결과를_반환한다() {
		PersonNormalization blankResult =
				personNormalizer.normalize(" ");

		assertEquals("", blankResult.normalizedText());
		assertEquals(
				List.of(),
				blankResult.strongMatchCandidates()
		);
		assertEquals(
				List.of(),
				blankResult.weakMatchCandidates()
		);

		PersonNormalization nullResult =
				personNormalizer.normalize(null);

		assertEquals("", nullResult.normalizedText());
		assertEquals(
				List.of(),
				nullResult.strongMatchCandidates()
		);
		assertEquals(
				List.of(),
				nullResult.weakMatchCandidates()
		);
	}
}