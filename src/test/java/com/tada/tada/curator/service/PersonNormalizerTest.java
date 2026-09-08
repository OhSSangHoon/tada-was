package com.tada.tada.curator.service;

import com.tada.tada.curator.model.PersonNormalization;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
				List.of("한영이와", "한영이", "한영"),
				result.strongMatchCandidates()
		);
	}

	@Test
	void 이름_뒤의_이랑을_강하게_정규화한다() {
		PersonNormalization result =
				personNormalizer.normalize("지훈이랑");

		assertEquals("지훈", result.normalizedText());
		assertEquals(
				List.of("지훈이랑", "지훈이", "지훈"),
				result.strongMatchCandidates()
		);
	}

	@Test
	void 이름_뒤의_이가를_강하게_정규화한다() {
		PersonNormalization result =
				personNormalizer.normalize("한영이가");

		assertEquals("한영", result.normalizedText());
		assertEquals(
				List.of("한영이가", "한영이", "한영"),
				result.strongMatchCandidates()
		);
	}

	@Test
	void 받침이_있는_이름의_은을_강하게_제거한다() {
		PersonNormalization result =
				personNormalizer.normalize("한영은");

		assertEquals(
				"한영",
				result.normalizedText()
		);

		assertEquals(
				List.of("한영은", "한영"),
				result.strongMatchCandidates()
		);

		assertEquals(
				List.of(),
				result.weakMatchCandidates()
		);
	}

	@Test
	void 받침이_없는_이름의_는을_강하게_제거한다() {
		PersonNormalization result =
				personNormalizer.normalize("민수는");

		assertEquals(
				"민수",
				result.normalizedText()
		);

		assertEquals(
				List.of("민수는", "민수"),
				result.strongMatchCandidates()
		);

		assertEquals(
				List.of(),
				result.weakMatchCandidates()
		);
	}

	@Test
	void 받침이_있는_이름의_이를_강하게_제거한다() {
		PersonNormalization result =
				personNormalizer.normalize("민혁이");

		assertEquals(
				"민혁",
				result.normalizedText()
		);

		assertEquals(
				List.of("민혁이", "민혁"),
				result.strongMatchCandidates()
		);

		assertEquals(
				List.of(),
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

		/*
		 * "박지은"은 흔한 성씨로 시작하는 세 글자 이름이므로
		 * 성 생략 변형 "지은"이 약한 후보로 생성된다.
		 * 약한 후보는 유사도 점수에만 쓰이고 자동 연결하지 않는다.
		 */
		assertEquals(
				List.of("지은"),
				result.weakMatchCandidates()
		);
	}

	@Test
	void 호칭은_매칭_후보로만_제거하고_표시_이름은_보존한다() {
		PersonNormalization result =
				personNormalizer.normalize("민수씨");

		assertEquals(
				"민수",
				result.normalizedText()
		);

		assertEquals(
				List.of("민수씨", "민수"),
				result.strongMatchCandidates()
		);

		assertEquals(
				"민수씨",
				result.displayNameCandidate()
		);
	}

	@Test
	void 조사와_호칭이_함께_있어도_단계적으로_후보를_만든다() {
		PersonNormalization result =
				personNormalizer.normalize("민수씨는");

		assertEquals(
				"민수",
				result.normalizedText()
		);

		assertEquals(
				List.of("민수씨는", "민수씨", "민수"),
				result.strongMatchCandidates()
		);

		assertEquals(
				"민수씨",
				result.displayNameCandidate()
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
	void 이름_끝일_수_있는_도는_매칭_후보로만_제거한다() {
		PersonNormalization result =
				personNormalizer.normalize("민수도");

		assertEquals(
				"민수",
				result.normalizedText()
		);

		assertEquals(
				List.of("민수도", "민수"),
				result.strongMatchCandidates()
		);

		assertEquals(
				"민수도",
				result.displayNameCandidate()
		);
	}

	@Test
	void 복합_조사를_끝까지_제거한다() {
		assertEquals(
				"민수",
				personNormalizer.normalize("민수와도").normalizedText()
		);

		assertEquals(
				"민수",
				personNormalizer.normalize("민수에게도").normalizedText()
		);

		PersonNormalization ambiguousSuffix =
				personNormalizer.normalize("민수랑도");

		assertEquals(
				"민수",
				ambiguousSuffix.normalizedText()
		);

		assertEquals(
				List.of("민수랑도", "민수랑", "민수"),
				ambiguousSuffix.strongMatchCandidates()
		);

		/*
		 * 확실한 조사만 붙은 "민수와도"는 표시 이름도 줄이지만
		 * 이름 끝일 수 있는 "랑도"는 표시 이름에서 보존한다.
		 */
		assertEquals(
				"민수",
				personNormalizer.normalize("민수와도").displayNameCandidate()
		);

		assertEquals(
				"민수랑도",
				ambiguousSuffix.displayNameCandidate()
		);
	}

	@Test
	void 이름_끝의_랑과_도는_표시_이름에서_제거하지_않는다() {
		/*
		 * "김사랑"은 이름 그 자체일 수도, "김사 + 랑"일 수도 있다.
		 * 매칭 후보에는 두 해석을 모두 넣되
		 * 표시 이름은 원문을 지켜 이름이 훼손되지 않게 한다.
		 */
		PersonNormalization love =
				personNormalizer.normalize("김사랑");

		assertEquals(
				"김사랑",
				love.displayNameCandidate()
		);

		assertEquals(
				List.of("김사랑", "김사"),
				love.strongMatchCandidates()
		);

		PersonNormalization island =
				personNormalizer.normalize("이영도");

		assertEquals(
				"이영도",
				island.displayNameCandidate()
		);

		assertEquals(
				List.of("이영도", "이영"),
				island.strongMatchCandidates()
		);
	}

	@Test
	void 기존_호환_메서드는_강한_후보만_반환한다() {
		assertEquals(
				List.of("한영이와", "한영이", "한영"),
				personNormalizer.normalizeCandidates(
						"한영이와"
				)
		);

		assertEquals(
				List.of("한영은", "한영"),
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
	void 유니코드_구두점도_정리한다() {
		assertEquals(
				"민수",
				personNormalizer.normalize("“민수”").normalizedText()
		);
		assertEquals(
				"민수",
				personNormalizer.normalize("「민수」。").normalizedText()
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

	@Test
	void 이름_끝일_수_있는_접미사는_표시_이름에서_제거하지_않는다() {
		/*
		 * 은 이 도 랑 님 씨 야 아 께 는
		 * 조사·호칭일 수도 실제 이름의 끝 글자일 수도 있다.
		 *
		 * 매칭 후보(normalizedText)에서는 제거하지만
		 * 신규 인물의 표시 이름에서는 원문을 지킨다.
		 */
		assertDisplayNameKept("김성은", "김성");
		assertDisplayNameKept("박정은", "박정");
		assertDisplayNameKept("가을이", "가을");
		assertDisplayNameKept("한영은", "한영");
		assertDisplayNameKept("민혁이", "민혁");
		assertDisplayNameKept("김사랑", "김사");
		assertDisplayNameKept("이영도", "이영");
		assertDisplayNameKept("민수도", "민수");
		assertDisplayNameKept("준호랑", "준호");
		assertDisplayNameKept("민혁아", "민혁");
		assertDisplayNameKept("김선아", "김선");
		assertDisplayNameKept("박민아", "박민");
		assertDisplayNameKept("민수님", "민수");

		assertEquals(
				"박지은",
				personNormalizer
						.normalize("박지은")
						.displayNameCandidate()
		);

		assertEquals(
				"박지은",
				personNormalizer
						.normalize("박지은")
						.normalizedText()
		);
	}

	private void assertDisplayNameKept(
			String rawText,
			String expectedNormalizedText
	) {
		PersonNormalization result =
				personNormalizer.normalize(rawText);

		assertEquals(
				rawText,
				result.displayNameCandidate()
		);

		assertEquals(
				expectedNormalizedText,
				result.normalizedText()
		);

		assertTrue(
				result.strongMatchCandidates()
						.contains(expectedNormalizedText)
		);
	}

	@Test
	void 확실한_조사는_표시_이름에서도_제거한다() {
		assertEquals(
				"민수",
				personNormalizer.normalize("민수와").displayNameCandidate()
		);

		assertEquals(
				"한영이",
				personNormalizer.normalize("한영이가").displayNameCandidate()
		);

		assertEquals(
				"영희",
				personNormalizer.normalize("영희에게서").displayNameCandidate()
		);

		assertEquals(
				"민수",
				personNormalizer.normalize("민수에게도").displayNameCandidate()
		);
	}

	@Test
	void 성_생략_변형은_흔한_성씨로_시작하는_세_글자_이름에만_만든다() {
		assertEquals(
				List.of("민혁"),
				personNormalizer.normalize("김민혁").weakMatchCandidates()
		);

		assertEquals(
				List.of("지은"),
				personNormalizer.normalize("박지은").weakMatchCandidates()
		);

		/*
		 * 첫 글자가 흔한 성씨가 아니면 성 생략형을 만들지 않는다.
		 * "가을이 -> 을이" 같은 잘못된 후보를 막는다.
		 */
		assertEquals(
				List.of(),
				personNormalizer.normalize("가을이").weakMatchCandidates()
		);

		assertEquals(
				List.of(),
				personNormalizer.normalize("사랑이").weakMatchCandidates()
		);

		assertEquals(
				List.of(),
				personNormalizer.normalize("민수").weakMatchCandidates()
		);
	}

	@Test
	void 구두점_앞에_공백이_있어도_조사를_제거한다() {
		/*
		 * 구두점을 제거한 뒤 꼬리 공백이 남으면
		 * endsWith 기반 조사 처리가 통째로 실패한다.
		 */
		assertEquals(
				"민수",
				personNormalizer
						.normalize("민수와 ,")
						.normalizedText()
		);

		assertEquals(
				"민수",
				personNormalizer
						.normalize("민수와 !!!")
						.normalizedText()
		);
	}

	@Test
	void 구두점이_아닌_기호도_정리한다() {
		assertEquals(
				"민수",
				personNormalizer
						.normalize("민수와♥")
						.normalizedText()
		);

		assertEquals(
				"민수",
				personNormalizer
						.normalize("민수와 ★")
						.normalizedText()
		);
	}

	@Test
	void 결합_조사는_한_번에_떼지_않아_이로_끝나는_이름을_지킨다() {
		/*
		 * "한영이가" 는 "한영 + 이가" 일 수도 "한영이 + 가" 일 수도 있다.
		 * "이가" 를 통째로 떼면 "가을이" 같은 이름이 "가을" 로 훼손된다.
		 *
		 * 단일 조사만 떼서 두 해석을 모두 후보로 남기고
		 * 표시 이름은 보수적인 쪽에서 멈춘다.
		 */
		PersonNormalization autumn =
				personNormalizer.normalize("가을이가");

		assertEquals(
				"가을이",
				autumn.displayNameCandidate()
		);

		assertEquals(
				"가을",
				autumn.normalizedText()
		);

		assertEquals(
				List.of("가을이가", "가을이", "가을"),
				autumn.strongMatchCandidates()
		);

		assertEquals(
				"준이",
				personNormalizer
						.normalize("준이가")
						.displayNameCandidate()
		);

		/*
		 * "사랑이랑" 은 "사랑 + 이랑" 또는 "사랑이 + 랑" 이다.
		 * 어느 쪽이든 "사랑이" 와 "사랑" 이 모두 후보로 남아
		 * 기존 인물이 있으면 연결된다.
		 */
		assertEquals(
				List.of("사랑이랑", "사랑이", "사랑"),
				personNormalizer
						.normalize("사랑이랑")
						.strongMatchCandidates()
		);
	}

	@Test
	void 부름말_야와_높임_조사_께는_표시_이름에서도_제거한다() {
		/*
		 * "야", "께" 는 이름의 끝 글자로 쓰이는 일이 사실상 없다.
		 */
		assertEquals(
				"영희",
				personNormalizer
						.normalize("영희야")
						.displayNameCandidate()
		);

		assertEquals(
				"민수",
				personNormalizer
						.normalize("민수야")
						.displayNameCandidate()
		);

		assertEquals(
				"선생님",
				personNormalizer
						.normalize("선생님께")
						.displayNameCandidate()
		);

		/*
		 * 같은 부름말이라도 "아" 는 실제 이름과 형태가 같아 보존한다.
		 */
		assertEquals(
				"김선아",
				personNormalizer
						.normalize("김선아")
						.displayNameCandidate()
		);

		assertEquals(
				"민혁아",
				personNormalizer
						.normalize("민혁아")
						.displayNameCandidate()
		);

		assertEquals(
				"민혁",
				personNormalizer
						.normalize("민혁아")
						.normalizedText()
		);
	}

	@Test
	void 호칭이_붙은_지칭어를_통째로_잘라내지_않는다() {
		assertEquals(
				"선생님",
				personNormalizer
						.normalize("선생님")
						.displayNameCandidate()
		);

		assertEquals(
				"아저씨",
				personNormalizer
						.normalize("아저씨")
						.displayNameCandidate()
		);

		assertEquals(
				"어머님",
				personNormalizer
						.normalize("어머님")
						.displayNameCandidate()
		);
	}

	@Test
	void 한_글자_인물_지칭어도_조사를_제거한다() {
		/*
		 * "남는 글자가 두 글자 이상일 때만 조사를 뗀다" 규칙 때문에
		 * "형과", "형은", "형이랑" 이 전부 다른 인물이 되던 문제.
		 *
		 * 한 글자 인물 지칭어는 닫힌 집합이므로 목록으로 예외를 둔다.
		 */
		for (String form : List.of(
				"형", "형과", "형을", "형은", "형이", "형도",
				"형에게", "형한테", "형에게서", "형하고",
				"형씨", "형님", "형아", "형이랑"
		)) {
			assertEquals(
					"형",
					personNormalizer
							.normalize(form)
							.normalizedText(),
					form
			);
		}

		assertEquals(
				"딸",
				personNormalizer.normalize("딸이랑").normalizedText()
		);

		assertEquals(
				"쌤",
				personNormalizer.normalize("쌤한테").normalizedText()
		);
	}

	@Test
	void 목록에_없는_한_글자는_조사로_보지_않아_이름이_지켜진다() {
		/*
		 * "가을" 의 "을" 을 목적격 조사로 오인하면 "가" 가 된다.
		 * "봄이", "별이" 도 마찬가지다.
		 */
		assertEquals(
				"가을",
				personNormalizer.normalize("가을").normalizedText()
		);

		assertEquals(
				"봄이",
				personNormalizer.normalize("봄이").normalizedText()
		);

		assertEquals(
				"별이",
				personNormalizer.normalize("별이").normalizedText()
		);

		assertEquals(
				"별이",
				personNormalizer.normalize("별이").displayNameCandidate()
		);
	}

	@Test
	void 호칭_앞의_공백을_정리한다() {
		/*
		 * 잘라낸 뒤 꼬리 공백이 남으면
		 * 이후 endsWith 기반 조사 처리가 전부 실패한다.
		 */
		assertEquals(
				"민수",
				personNormalizer.normalize("민수 씨").normalizedText()
		);

		assertEquals(
				"민수",
				personNormalizer.normalize("민수 님").normalizedText()
		);

		assertEquals(
				"민수 씨",
				personNormalizer
						.normalize("민수 씨")
						.displayNameCandidate()
		);
	}

	@Test
	void 꼬리에_붙은_한글_자모를_정리한다() {
		assertEquals(
				"민수",
				personNormalizer.normalize("민수ㅋㅋ").normalizedText()
		);

		assertEquals(
				"민수",
				personNormalizer.normalize("민수ㅠㅠ").normalizedText()
		);

		assertEquals(
				"",
				personNormalizer.normalize("ㅁㅅ").normalizedText()
		);
	}

	/*
	 * cleanText 는 앞뒤 구두점을 한 번만 제거한다.
	 * 조사를 떼면 그 앞에 있던 구두점이 새로 꼬리가 되므로
	 * 조사 제거 지점에서 한 번 더 정리해야 한다.
	 *
	 * 정리하지 않으면 "민수,가" 가 "민수," 로 남아
	 * 기존 인물 "민수" 와 매칭되지 않고 새 MemoryPerson 이 생긴다.
	 */
	@Test
	void 조사를_떼면_드러나는_구두점을_정리한다() {
		assertEquals(
				"민수",
				personNormalizer.normalize("민수,가").normalizedText()
		);

		assertEquals(
				"민수",
				personNormalizer.normalize("(민수)는").normalizedText()
		);

		assertEquals(
				"민수",
				personNormalizer.normalize("민수(가)").normalizedText()
		);

		assertEquals(
				"민수",
				personNormalizer.normalize("\"민수\"는").normalizedText()
		);

		assertEquals(
				"민수",
				personNormalizer.normalize("민수-는").displayNameCandidate()
		);
	}

	/*
	 * 구두점이 남으면 hasBatchim 이 마지막 글자를 구두점으로 보아
	 * 받침 판정이 항상 false 가 된다.
	 * "은" 은 받침이 있어야 붙는 조사이므로 제거되지 못한다.
	 */
	@Test
	void 구두점_정리_후_받침_판정이_정상_동작한다() {
		assertEquals(
				"지훈",
				personNormalizer.normalize("(지훈)은").normalizedText()
		);

		assertEquals(
				"지훈",
				personNormalizer.normalize("지훈,이").normalizedText()
		);
	}

	/*
	 * 구두점 정리가 실제 이름을 훼손하면 안 된다.
	 */
	@Test
	void 구두점_정리가_이름을_훼손하지_않는다() {
		assertEquals(
				"김성은",
				personNormalizer.normalize("김성은").displayNameCandidate()
		);

		assertEquals(
				"이영도",
				personNormalizer.normalize("이영도").displayNameCandidate()
		);

		assertEquals(
				"박지은",
				personNormalizer.normalize("박지은").normalizedText()
		);
	}
}
