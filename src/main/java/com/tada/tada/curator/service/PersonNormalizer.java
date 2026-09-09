package com.tada.tada.curator.service;

import com.tada.tada.curator.model.PersonNormalization;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class PersonNormalizer {

	/*
	 * 인구 기준 상위 성씨. 이름 첫 글자로도 흔히 쓰이는
	 * "가", "사", "라" 등은 오탐을 막기 위해 넣지 않는다.
	 */
	/*
	 * 조사를 떼면 한 글자만 남는, 뜻이 확정된 표현들.
	 *
	 * 기본 규칙은 "남는 글자가 두 글자 이상일 때만 조사를 뗀다" 이다.
	 * 이 규칙이 "가을 -> 가", "봄이 -> 봄" 같은 이름 훼손을 막는다.
	 *
	 * 한국어에서 한 글자로 쓰이는 인물 지칭어와 대명사는 닫힌 집합이라
	 * 이 목록에 한해 예외를 둔다. 목록에 없으면 그대로 두므로
	 * 새 이름을 훼손할 위험이 없다.
	 *
	 *   형 딸 쌤 샘 : 실제 인물 지칭. 조사별로 인물이 갈리는 것을 막는다.
	 *   그 걔 쟤 얘 나 너 저 : 대명사. 조사를 떼야
	 *                        ExtractionValidator 가 인물 아님으로 거를 수 있다.
	 */
	private static final Set<String> ONE_CHAR_ALLOWED_BASES =
			Set.of(
					"형", "딸", "쌤", "샘",
					"그", "걔", "쟤", "얘", "나", "너", "저"
			);

	private static final int SURNAME_VARIANT_NAME_LENGTH = 3;

	private static final int MIN_BASE_LENGTH_AFTER_PARTICLE = 2;

	private static final Set<String> COMMON_SURNAMES =
			Set.of(
					"김", "이", "박", "최", "정",
					"강", "조", "윤", "장", "임",
					"한", "오", "서", "신", "권",
					"황", "안", "송", "류", "전",
					"홍", "고", "문", "손", "양",
					"배", "백", "허", "남", "심",
					"노", "하", "곽", "성", "차",
					"주", "우", "구", "민", "유",
					"진", "지", "엄", "채", "원",
					"천", "방", "공", "현", "함",
					"변", "염", "여", "추", "소",
					"석", "선", "설", "마", "길",
					"연", "위", "표", "명", "기",
					"반", "왕", "금", "옥", "육",
					"인", "제", "탁", "국", "어",
					"은", "편", "용", "봉", "태"
			);


	public PersonNormalization normalize(String rawText) {
		if (rawText == null || rawText.isBlank()) {
			return emptyNormalization();
		}

		String cleanedText = cleanText(rawText);

		if (cleanedText.isBlank()) {
			return emptyNormalization();
		}

		MatchCandidates matchCandidates =
				createMatchCandidates(cleanedText);

		Set<String> strongMatchCandidates =
				matchCandidates.strong();

		Set<String> safeMatchCandidates =
				matchCandidates.safe();

		String normalizedText =
				strongMatchCandidates.stream()
						.reduce((first, second) -> second)
						.orElse(cleanedText);

		String displayNameCandidate =
				createDisplayNameCandidate(cleanedText);

		Set<String> weakMatchCandidates =
				createWeakMatchCandidates(
						normalizedText,
						strongMatchCandidates
				);

		return new PersonNormalization(
				normalizedText,
				displayNameCandidate,
				List.copyOf(strongMatchCandidates),
				List.copyOf(safeMatchCandidates),
				List.copyOf(weakMatchCandidates)
		);
	}

	public String normalizeName(String rawText) {
		return normalize(rawText).normalizedText();
	}

	public List<String> normalizeCandidates(String rawText) {
		return normalize(rawText).strongMatchCandidates();
	}

	private PersonNormalization emptyNormalization() {
		return new PersonNormalization(
				"",
				"",
				List.of(),
				List.of(),
				List.of()
		);
	}

	private String cleanText(String rawText) {
		String normalized = Normalizer.normalize(
				rawText,
				Normalizer.Form.NFKC
		);

		/*
		 * 구두점과 기호를 제거한 뒤 다시 trim 한다.
		 *
		 * "민수와 ," 처럼 구두점 앞에 공백이 있으면
		 * 제거 후 꼬리 공백이 남아 endsWith 기반 조사 처리가
		 * 통째로 실패한다.
		 */
		/*
		 * NFKC 는 "ㅋㅋ", "ㅠㅠ" 같은 호환 자모를 조합용 자모로 바꾼다.
		 * 자모만 남은 조각은 이름이 될 수 없으므로 앞뒤에서 제거한다.
		 * 전부 자모였다면 빈 문자열이 되어 Extraction 검증에서 걸린다.
		 */
		return normalized
				.trim()
				.replaceAll("\\s+", " ")
				.replaceAll(
						"^[\\p{P}\\p{S}\\u1100-\\u11FF\\u3130-\\u318F]+"
								+ "|[\\p{P}\\p{S}\\u1100-\\u11FF\\u3130-\\u318F]+$",
						""
				)
				.trim();
	}

	/*
	 * 매칭 후보 두 겹을 함께 만든다.
	 *
	 *   strong : 안전 조사 -> 애매한 조사 순으로 반복해서 뗀 전체 사슬.
	 *            normalizedText 와 저장값 계열은 이 결과를 그대로 쓴다.
	 *   safe   : 그 사슬 중 안전 조사 제거만으로 설명되는 앞부분만 남긴
	 *            부분집합.
	 *
	 * "김성은" 처럼 애매한 조사(은/이/도/랑/님/씨/아)를 떼야만 도달하는
	 * 형태는 strong 에는 남기되 safe 에서는 뺀다.
	 *
	 * 한 번이라도 애매한 조사를 거치면 그 뒤로 이어지는 모든 형태를
	 * safe 에서 제외한다(stillSafe). 애매한 조사로 이미 다른 해석에
	 * 들어선 뒤에 우연히 안전 조사 모양이 다시 나타나도, 그 형태는
	 * 애매한 전제 위에 서 있으므로 EXACT 매칭까지 신뢰할 수는 없다.
	 *
	 * safe 는 PersonMatchingService 의 EXACT 판정에만 쓰고, strong 에서
	 * safe 를 뺀 나머지는 유사도 점수로만 반영한다. 그래야 "김성은/김성",
	 * "가을/가을이" 처럼 서로 다를 수 있는 사람이 애매한 조사 하나 때문에
	 * 자동으로 합쳐지는 일을 막을 수 있다.
	 */
	private MatchCandidates createMatchCandidates(
			String cleanedText
	) {
		Set<String> strongCandidates =
				new LinkedHashSet<>();

		Set<String> safeCandidates =
				new LinkedHashSet<>();

		String candidate = cleanedText;
		strongCandidates.add(candidate);
		safeCandidates.add(candidate);

		boolean stillSafe = true;

		while (true) {
			String next =
					removeStrongParticleOnce(candidate);

			if (next.equals(candidate)) {
				break;
			}

			strongCandidates.add(next);

			if (stillSafe) {
				String safeNext =
						removeSafeParticleOnce(candidate);

				if (safeNext.equals(next)) {
					safeCandidates.add(next);
				} else {
					stillSafe = false;
				}
			}

			candidate = next;
		}

		return new MatchCandidates(
				strongCandidates,
				safeCandidates
		);
	}

	private record MatchCandidates(
			Set<String> strong,
			Set<String> safe
	) {
	}

	/*
	 * 신규 MemoryPerson 에게 보여 줄 이름 후보를 만든다.
	 *
	 * "은", "이" 는 조사일 수도 있고 실제 이름의 마지막 글자일 수도 있어
	 * 문자열만으로는 구분할 수 없다.
	 *
	 *   "한영은"  = 한영 + 조사 은
	 *   "김성은"  = 이름 그 자체
	 *   "가을이"  = 이름 그 자체
	 *
	 * 매칭 후보에서는 두 해석을 모두 시도하지만,
	 * 표시 이름에는 애매한 접미사 제거를 반영하지 않는다.
	 * 실제 이름을 훼손하는 쪽이 조사가 남는 쪽보다 위험하기 때문이다.
	 *
	 * 조사가 남은 표시 이름은 사용자가 직접 교정할 수 있지만,
	 * 훼손된 이름은 다른 사람과의 잘못된 병합으로 이어진다.
	 */
	private String createDisplayNameCandidate(
			String cleanedText
	) {
		String candidate = cleanedText;

		while (true) {
			String next =
					removeSafeParticleOnce(candidate);

			if (next.equals(candidate)) {
				break;
			}

			candidate = next;
		}

		return candidate;
	}

	/*
	 * 약한 후보 = 성 생략 변형.
	 *
	 * 명세 9.3 이 STRONG 근거로 든 "유일한 성 포함/생략 변형"을
	 * 유사도 점수 계산에만 사용한다. 단독으로 자동 연결하지 않는다.
	 *
	 *   김민혁 -> 민혁
	 *   박지은 -> 지은
	 *
	 * 첫 글자를 무조건 떼면 "가을이 -> 을이" 같은 잘못된 후보가 생겨
	 * 실제로 "을이"라는 인물과 오매칭될 수 있다.
	 * 그래서 흔한 성씨로 시작하는 세 글자 이름에만 적용한다.
	 *
	 * 중간 조사 제거형이 아니라 저장값과 표시 이름 후보에서만 만든다.
	 */
	private Set<String> createWeakMatchCandidates(
			String normalizedText,
			Set<String> strongMatchCandidates
	) {
		Set<String> weakCandidates =
				new LinkedHashSet<>();

		/*
		 * 조사·호칭을 모두 뗀 저장값에서만 만든다.
		 *
		 * 표시 이름 후보("민수도", "민수씨")에서 만들면
		 * "수도", "수씨" 같은 잘못된 후보가 나온다.
		 */
		String variant =
				removeSurname(normalizedText);

		if (!variant.equals(normalizedText)) {
			weakCandidates.add(variant);
		}

		weakCandidates.removeAll(
				strongMatchCandidates
		);

		return weakCandidates;
	}

	/*
	 * 한국인 이름은 대부분 "성 1글자 + 이름 2글자" 형태다.
	 *
	 * 흔한 성씨로 시작하는 세 글자 이름일 때만 성 생략형을 만든다.
	 * 목록에 없는 첫 글자는 성으로 보지 않으므로
	 * "가을이", "사랑이" 같은 이름은 변형을 만들지 않는다.
	 */
	public String removeSurname(String text) {
		if (text == null) {
			return "";
		}

		if (text.length() != SURNAME_VARIANT_NAME_LENGTH) {
			return text;
		}

		if (!COMMON_SURNAMES.contains(
				text.substring(0, 1)
		)) {
			return text;
		}

		return text.substring(1);
	}

	private String removeStrongParticleOnce(
			String text
	) {
		return removeStrongParticleExceptDo(
				text
		);
	}

	private String removeSafeParticleOnce(
			String text
	) {
		String result =
				removeSimpleParticle(
						text,
						"도"
				);

		if (!result.equals(text)
				&& !removeSafeParticleExceptDo(result)
				.equals(result)) {

			return result;
		}

		return removeSafeParticleExceptDo(
				text
		);
	}

	private String removeStrongParticleExceptDo(
			String text
	) {
		String result =
				removeSafeParticleExceptDo(text);

		if (!result.equals(text)) {
			return result;
		}

		return removeAmbiguousParticle(text);
	}

	/*
	 * 이름 마지막 글자로 쓰일 가능성이 낮은 조사만 제거한다.
	 * 표시 이름 후보와 강한 매칭 후보 양쪽에서 사용한다.
	 *
	 * "이와", "이랑", "이가" 같은 결합형은 여기서 다루지 않는다.
	 * 앞의 "이"가 이름의 끝 글자일 수 있기 때문이다.
	 *
	 *   한영이가 = 한영 + 이가
	 *   가을이가 = 가을이 + 가
	 *
	 * 단일 조사만 떼면 두 해석이 모두 후보로 남는다.
	 *   가을이가 -> 가을이 -> 가을
	 * 표시 이름은 보수적으로 "가을이"에서 멈춘다.
	 */
	private String removeSafeParticleExceptDo(
			String text
	) {
		String result;

		result = removeSimpleParticle(
				text,
				"에게서"
		);
		if (!result.equals(text)) return result;

		result = removeSimpleParticle(
				text,
				"한테서"
		);
		if (!result.equals(text)) return result;

		result = removeSimpleParticle(
				text,
				"에게"
		);
		if (!result.equals(text)) return result;

		result = removeSimpleParticle(
				text,
				"한테"
		);
		if (!result.equals(text)) return result;

		result = removeSimpleParticle(
				text,
				"하고"
		);
		if (!result.equals(text)) return result;

		result = removeParticleWithBatchim(
				text,
				"와",
				false
		);
		if (!result.equals(text)) return result;

		result = removeParticleWithBatchim(
				text,
				"를",
				false
		);
		if (!result.equals(text)) return result;

		result = removeParticleWithBatchim(
				text,
				"과",
				true
		);
		if (!result.equals(text)) return result;

		result = removeParticleWithBatchim(
				text,
				"을",
				true
		);
		if (!result.equals(text)) return result;

		result = removeParticleWithBatchim(
				text,
				"는",
				false
		);
		if (!result.equals(text)) return result;

		result = removeParticleWithBatchim(
				text,
				"가",
				false
		);
		if (!result.equals(text)) return result;

		/*
		 * 부름말 "야" 와 높임 조사 "께" 는
		 * 이름의 끝 글자로 쓰이는 일이 사실상 없어 안전하게 뗀다.
		 *
		 * 같은 부름말이라도 "아" 는 "김선아", "박민아" 처럼
		 * 실제 이름과 형태가 같아 애매한 접미사로 남긴다.
		 */
		result = removeParticleWithBatchim(
				text,
				"야",
				false
		);
		if (!result.equals(text)) return result;

		return removeSimpleParticle(
				text,
				"께"
		);
	}

	/*
	 * 조사·호칭일 수도 있고 실제 이름의 끝 글자일 수도 있는 접미사다.
	 *
	 *   은 : "한영은"(조사) / "김성은"(이름)
	 *   이 : "민혁이"(조사) / "가을이"(이름)
	 *   도 : "민수도"(조사) / "이영도"(이름)
	 *   랑 : "민수랑"(조사) / "김사랑"(이름)
	 *   씨 님 : 호칭 / "아저씨", "선생님" 같은 지칭
	 *   아 : 부름말 / "김선아", "박민아" 같은 이름 끝 글자
	 *
	 * 문자열만으로는 구분할 수 없으므로
	 * 매칭 후보 확장에만 사용하고 표시 이름에는 반영하지 않는다.
	 *
	 * 각 접미사는 마지막 글자가 서로 달라 동시에 성립하지 않는다.
	 */
	private String removeAmbiguousParticle(
			String text
	) {
		String result;

		result = removeParticleWithBatchim(
				text,
				"은",
				true
		);
		if (!result.equals(text)) return result;

		result = removeParticleWithBatchim(
				text,
				"이",
				true
		);
		if (!result.equals(text)) return result;

		result = removeSimpleParticle(
				text,
				"도"
		);
		if (!result.equals(text)) return result;

		result = removeParticleWithBatchim(
				text,
				"랑",
				false
		);
		if (!result.equals(text)) return result;

		result = removeSimpleParticle(
				text,
				"님"
		);
		if (!result.equals(text)) return result;

		result = removeSimpleParticle(
				text,
				"씨"
		);
		if (!result.equals(text)) return result;

		return removeParticleWithBatchim(
				text,
				"아",
				true
		);
	}

	private String removeSimpleParticle(
			String text,
			String particle
	) {
		if (!text.endsWith(particle)) {
			return text;
		}

		/*
		 * "민수 씨" 처럼 조사·호칭 앞에 공백이 있으면
		 * 잘라낸 뒤 꼬리 공백이 남아 이후 endsWith 판정이 전부 실패한다.
		 */
		String base =
				text.substring(
						0,
						text.length()
								- particle.length()
				).strip();

		/*
		 * 조사를 떼면 그 앞에 숨어 있던 구두점이 꼬리로 드러난다.
		 *
		 *   "민수,가"   -> "민수,"
		 *   "(민수)는"  -> "민수)"
		 *
		 * 이대로 두면 두 가지가 깨진다.
		 * 1. "민수" 와 다른 값이 되어 같은 사람이 새 MemoryPerson 으로 갈라진다.
		 * 2. hasBatchim 이 마지막 글자를 구두점으로 보아 받침 판정이 항상 false 가 된다.
		 *
		 * cleanText 의 꼬리 규칙을 여기서 한 번 더 적용한다.
		 * 한글 음절은 이 문자 집합에 포함되지 않으므로 이름이 잘리지 않는다.
		 */
		base = base
				.replaceAll(
						"[\\p{P}\\p{S}\\u1100-\\u11FF\\u3130-\\u318F]+$",
						""
				)
				.strip();

		if (base.isEmpty()) {
			return text;
		}

		return base.length() >= MIN_BASE_LENGTH_AFTER_PARTICLE
				|| ONE_CHAR_ALLOWED_BASES.contains(base)
				? base
				: text;
	}

	private String removeParticleWithBatchim(
			String text,
			String particle,
			boolean requiresBatchim
	) {
		String base =
				removeSimpleParticle(
						text,
						particle
				);

		if (base.equals(text)) {
			return text;
		}

		return hasBatchim(base)
				== requiresBatchim
				? base
				: text;
	}

	private boolean hasBatchim(String text) {
		char lastChar =
				text.charAt(
						text.length() - 1
				);

		if (lastChar < '가'
				|| lastChar > '힣') {
			return false;
		}

		return (lastChar - '가') % 28 != 0;
	}
}
