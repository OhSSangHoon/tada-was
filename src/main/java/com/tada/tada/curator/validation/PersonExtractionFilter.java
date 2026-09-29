package com.tada.tada.curator.validation;

import com.tada.tada.curator.model.PersonNormalization;
import com.tada.tada.curator.service.PersonNormalizer;
import com.tada.tada.global.event.dto.PersonExtraction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PersonExtractionFilter {

	private static final Set<String> NON_PERSON_TERMS =
			Set.of(
					"그", "그녀", "그들", "그분", "그애", "그애들",
					"걔", "쟤", "얘", "걔네", "쟤네", "얘네",
					"나", "너", "저", "우리", "저희", "너희", "당신",
					"자기", "본인", "누구", "아무", "아무나", "아무도",
					"사람", "사람들", "이들", "저들",
					"다", "다들", "모두", "모두들", "여럿", "여러분",
					"애들", "얘들", "친구들", "동료들", "팀원들",
					"가족", "가족들", "식구", "식구들", "친척", "친척들",
					"형제", "자매", "남매", "부모", "부모님",
					"어른들", "손님들", "사람들끼리",
					"전부", "다같이", "다함께", "모두다", "둘다", "셋다",
					"함께", "같이", "서로", "몇몇",
					"다른사람", "다른사람들", "누군가"
			);

	/*
	 * 단순 contains/완전일치만으로는
	 * "동네친구들", "회사동료들" 같은 복합 집단 표현을 잡지 못한다.
	 *
	 * "들" 하나만 검사하면 "아들" 같은 실제 한 사람 지칭까지
	 * 잘못 제외할 수 있으므로 구체적인 집단 명사만 suffix로 관리한다.
	 */
	private static final Set<String> PLURAL_GROUP_SUFFIXES =
			Set.of(
					"친구들",
					"친구분들",
					"동료들",
					"팀원들",
					"사람들",
					"가족들",
					"가족분들",
					"식구들",
					"친척들",
					"애들",
					"얘들",
					"아이들",
					"어른들",
					"손님들",
					"선배들",
					"후배들",
					"동기들",
					"멤버들",
					"일행들",
					"직원들",
					"학생들",
					"지인들",
					"주민들",
					"사촌들",
					"형제들",
					"자매들",
					"부모님들"
			);

	private static final Set<String> PRONOUN_STEMS =
			Set.of(
					"우리", "저희", "너희", "그", "걔", "쟤", "얘",
					"나", "너", "저", "당신", "자기", "본인", "누구", "아무"
			);

	private static final Set<String> PRONOUN_PLURAL_SUFFIXES =
			Set.of("들", "네", "끼리");

	private static final Set<String> GROUP_OWNERS =
			Set.of("우리", "저희", "너희", "내", "제");

	private static final Set<String> GROUP_HEADS =
			Set.of(
					"가족", "식구", "친척", "팀", "반", "조", "그룹",
					"모임", "동아리", "동기", "멤버", "일행", "패거리",
					"형제", "자매", "남매", "부모", "부모님",
					"친구들", "사람들", "애들", "얘들",
					"동료들", "팀원들", "가족들"
			);

	private static final Set<String> DETERMINERS =
			Set.of("그", "저", "이", "다른", "어떤", "무슨", "웬");

	private static final Set<String> GENERIC_HEADS =
			Set.of(
					"사람", "사람들",
					"애", "애들",
					"얘", "얘들",
					"분", "분들",
					"친구", "친구들",
					"녀석", "녀석들",
					"여자", "남자",
					"아이", "아이들"
			);

	private static final Set<String> QUANTITY_WORDS =
			Set.of(
					"한", "두", "세", "네", "다섯",
					"여섯", "일곱", "여덟", "아홉", "열",
					"둘", "셋", "넷",
					"여러", "몇", "몇몇", "여럿",
					"수", "수십", "온"
			);

	private static final Set<String> COUNT_UNITS =
			Set.of(
					"명",
					"분",
					"사람",
					"이서",
					"커플",
					"쌍"
			);

	private static final Set<String> COUNT_EXPRESSION_PERSON_EXCEPTIONS =
			Set.of(
					"한이서"
			);

	private final PersonNormalizer personNormalizer;

	public Set<String> findExcludedRefs(
			List<PersonExtraction> persons
	) {
		Set<String> excludedRefs =
				new HashSet<>();

		for (PersonExtraction person : persons) {

			if (shouldExclude(
					person.rawText()
			)) {
				excludedRefs.add(
						person.ref()
				);
			}
		}

		return excludedRefs;
	}

	public boolean shouldExclude(
			String rawText
	) {
		if (rawText == null
				|| rawText.isBlank()) {
			return false;
		}

		PersonNormalization normalization =
				personNormalizer.normalize(
						rawText
				);

		return isNonPersonTerm(
				rawText
		)
				|| isNonPersonTerm(
				normalization.normalizedText()
		)
				|| isNonPersonTerm(
				normalization.displayNameCandidate()
		);
	}

	private boolean isNonPersonTerm(
			String value
	) {
		if (value == null
				|| value.isBlank()) {
			return false;
		}

		String compact =
				value.replace(
						" ",
						""
				);

		if (compact.isEmpty()) {
			return false;
		}

		return NON_PERSON_TERMS.contains(
				compact
		)
				|| endsWithPluralGroup(
				compact
		)
				|| isPronounPlural(
				compact
		)
				|| isPronounGroup(
				compact
		)
				|| isDeterminerGeneric(
				compact
		)
				|| isCountExpression(
				compact
		);
	}

	private boolean endsWithPluralGroup(
			String compact
	) {
		for (String suffix
				: PLURAL_GROUP_SUFFIXES) {

			if (compact.endsWith(
					suffix
			)) {
				return true;
			}
		}

		return false;
	}

	private boolean isPronounPlural(
			String compact
	) {
		for (String suffix
				: PRONOUN_PLURAL_SUFFIXES) {

			String stem =
					stripSuffix(
							compact,
							suffix
					);

			if (stem == null) {
				continue;
			}

			if (PRONOUN_STEMS.contains(
					stem
			)
					|| NON_PERSON_TERMS.contains(
					stem
			)) {
				return true;
			}
		}

		return false;
	}

	private boolean isPronounGroup(
			String compact
	) {
		for (String owner
				: GROUP_OWNERS) {

			String head =
					stripPrefix(
							compact,
							owner
					);

			if (head == null) {
				continue;
			}

			if (GROUP_HEADS.contains(
					head
			)
					|| NON_PERSON_TERMS.contains(
					head
			)
					|| endsWithPluralGroup(
					head
			)) {
				return true;
			}
		}

		return false;
	}

	private boolean isDeterminerGeneric(
			String compact
	) {
		for (String determiner
				: DETERMINERS) {

			String head =
					stripPrefix(
							compact,
							determiner
					);

			if (head != null
					&& GENERIC_HEADS.contains(
					head
			)) {
				return true;
			}
		}

		return false;
	}

	private boolean isCountExpression(
			String compact
	) {
		if (COUNT_EXPRESSION_PERSON_EXCEPTIONS
				.contains(
						compact
				)) {
			return false;
		}

		for (String unit
				: COUNT_UNITS) {

			String quantity =
					stripSuffix(
							compact,
							unit
					);

			if (quantity == null) {
				continue;
			}

			if (QUANTITY_WORDS.contains(
					quantity
			)
					|| isAllDigits(
					quantity
			)) {
				return true;
			}
		}

		return false;
	}

	private String stripPrefix(
			String compact,
			String prefix
	) {
		return compact.length()
				> prefix.length()
				&& compact.startsWith(
				prefix
		)
				? compact.substring(
				prefix.length()
		)
				: null;
	}

	private String stripSuffix(
			String compact,
			String suffix
	) {
		return compact.length()
				> suffix.length()
				&& compact.endsWith(
				suffix
		)
				? compact.substring(
				0,
				compact.length()
						- suffix.length()
		)
				: null;
	}

	private boolean isAllDigits(
			String value
	) {
		if (value.isEmpty()) {
			return false;
		}

		for (int index = 0;
			 index < value.length();
			 index++) {

			if (!Character.isDigit(
					value.charAt(index)
			)) {
				return false;
			}
		}

		return true;
	}
}
