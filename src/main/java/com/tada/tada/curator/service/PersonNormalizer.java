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
	 * 인구 기준 상위 성씨. "가/사/라" 등 이름 첫 글자로 흔한 성은 오탐 방지로 제외한다.
	 */
	/*
	 * 조사를 떼면 한 글자만 남는 표현들. 기본 규칙은 "남는 글자 2자 이상일 때만 조사 제거"
	 * (예: "가을→가" 훼손 방지). 한 글자로 쓰이는 인물 지칭·대명사는 닫힌 집합이라 예외로 허용한다.
	 *   형/딸/쌤/샘: 실제 인물 지칭 (조사별로 인물이 갈리는 것 방지)
	 *   그/걔/쟤/얘/나/너/저: 대명사 (조사를 떼야 ExtractionValidator가 인물 아님으로 판별 가능)
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
		 * 구두점 제거 후 재trim한다 — "민수와 ," 처럼 구두점 앞 공백이 남으면
		 * 이후 endsWith 기반 조사 처리가 통째로 실패한다.
		 */
		/*
		 * NFKC로 호환 자모("ㅋㅋ" 등)를 정규화 후 제거한다 — 자모만 남은 조각은 이름이 될 수 없고,
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
	 * 매칭 후보 두 겹: strong(안전+애매한 조사를 순서대로 반복 제거한 전체 사슬, normalizedText가
	 * 이 값을 씀)과 safe(그 사슬 중 안전 조사 제거만으로 설명되는 부분집합). "김성은"처럼 애매한
	 * 조사(은/이/도/랑/님/씨/아)를 떼야만 나오는 형태는 strong에는 남기고 safe에서는 뺀다.
	 * 한 번이라도 애매한 조사를 거치면 그 뒤 모든 형태를 safe에서 영구히 제외한다(stillSafe) —
	 * 이미 애매한 해석에 들어선 뒤 우연히 안전 조사 모양이 나와도 신뢰할 수 없기 때문이다.
	 * safe는 PersonMatchingService의 EXACT 판정에만 쓰고 나머지는 유사도 점수로만 반영해,
	 * "김성은/김성" 같은 서로 다를 수 있는 사람이 자동으로 합쳐지는 것을 막는다.
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
	 * 표시 이름 후보. "은","이"는 조사(한영은=한영+은)일 수도 이름 끝 글자(김성은, 가을이)일 수도
	 * 있어 매칭 후보에서는 두 해석을 다 시도하지만, 표시 이름에는 반영하지 않는다 — 이름을 잘못
	 * 훼손하는 쪽이 조사가 남는 쪽보다 위험하다 (후자는 사용자가 직접 교정 가능).
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
	 * 약한 후보 = 성 생략 변형 (명세 9.3 STRONG 근거, 유사도 점수 전용, 단독 자동연결 안 함).
	 * 예: 김민혁→민혁. 첫 글자를 무조건 떼면 "가을이→을이" 같은 오탐이 생기므로, 흔한 성씨로
	 * 시작하는 세 글자 이름에만 적용한다. 저장값·표시 이름 후보에서만 만든다.
	 */
	private Set<String> createWeakMatchCandidates(
			String normalizedText,
			Set<String> strongMatchCandidates
	) {
		Set<String> weakCandidates =
				new LinkedHashSet<>();

		/*
		 * 조사·호칭을 모두 뗀 저장값에서만 만든다 — 표시 이름 후보("민수도","민수씨")에서
		 * 만들면 "수도","수씨" 같은 잘못된 후보가 나온다.
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
	 * 흔한 성씨로 시작하는 3글자 이름에만 적용한다 (목록에 없는 첫 글자는 성으로 보지 않아
	 * "가을이","사랑이"는 보존된다).
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
	 * 이름 끝 글자로 쓰일 가능성이 낮은 조사만 제거한다 (표시 이름·강한 매칭 후보 공용).
	 * "이와","이랑","이가" 같은 결합형은 다루지 않는다 — 앞의 "이"가 이름 끝 글자일 수 있기
	 * 때문이다 (한영이가=한영+이가, 가을이가=가을이+가). 표시 이름은 보수적으로 "가을이"에서 멈춘다.
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
		 * "야","께"는 이름 끝 글자로 쓰이는 일이 거의 없어 안전하게 제거하지만, "아"는
		 * "김선아","박민아"처럼 실제 이름과 형태가 같아 애매한 접미사로 남긴다.
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
	 * 조사·호칭일 수도, 이름 끝 글자일 수도 있어 매칭 후보 확장에만 쓰고 표시 이름에는 반영하지 않는다.
	 *   은: 한영은(조사)/김성은(이름)   이: 민혁이(조사)/가을이(이름)   도: 민수도(조사)/이영도(이름)
	 *   랑: 민수랑(조사)/김사랑(이름)   씨·님: 호칭   아: 부름말/김선아·박민아(이름)
	 * 각 접미사는 마지막 글자가 달라 동시에 성립하지 않는다.
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
		 * "민수 씨"처럼 조사 앞 공백이 있으면 자른 뒤 공백이 남아 이후 endsWith 판정이 전부 실패한다.
		 */
		String base =
				text.substring(
						0,
						text.length()
								- particle.length()
				).strip();

		/*
		 * 조사를 떼면 숨어 있던 구두점이 꼬리로 드러난다 (예: "민수,가"→"민수,", "(민수)는"→"민수)").
		 * 방치하면 같은 사람이 다른 값으로 갈라지거나 hasBatchim이 구두점을 마지막 글자로
		 * 오판하므로, cleanText의 꼬리 규칙을 한 번 더 적용한다. 한글 음절은 이 문자 집합에
		 * 없어 이름은 잘리지 않는다.
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
