package com.tada.tada.curator.validation;

import com.tada.tada.curator.entity.MentionEntityType;
import com.tada.tada.curator.exception.ExtractionValidationException;
import com.tada.tada.curator.model.PersonNormalization;
import com.tada.tada.curator.service.PersonNormalizer;
import com.tada.tada.global.event.dto.ActivityExtraction;
import com.tada.tada.global.event.dto.ExtractionResult;
import com.tada.tada.global.event.dto.PersonExtraction;
import com.tada.tada.global.event.dto.PlaceExtraction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ExtractionResultValidator {

	/*
	 * 대명사·불특정 복수 지칭은 인물 카드가 될 수 없다 (명세 8.2-6). "엄마","동생"처럼 특정
	 * 인물을 가리킬 수 있는 단수 지칭은 정상 인물로 허용한다.
	 */
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
	 * 완전일치만으로는 "우리"는 막고 "우리들"은 놓친다 — 아래 네 결합 패턴을 추가 판정한다 (명세 5.3).
	 */

	/** 대명사 어간. 복수·한정 접미사와 결합하면 거부한다. */
	private static final Set<String> PRONOUN_STEMS =
			Set.of(
					"우리", "저희", "너희", "그", "걔", "쟤", "얘",
					"나", "너", "저", "당신", "자기", "본인", "누구", "아무"
			);

	private static final Set<String> PRONOUN_PLURAL_SUFFIXES =
			Set.of("들", "네", "끼리");

	/** 소유 대명사. 뒤에 사람 집합 명사가 붙으면 거부한다. */
	private static final Set<String> GROUP_OWNERS =
			Set.of("우리", "저희", "너희", "내", "제");

	/** 집합 명사만. "엄마" 같은 단수 호칭을 넣으면 "우리 엄마" 가 막힌다. */
	private static final Set<String> GROUP_HEADS =
			Set.of(
					"가족", "식구", "친척", "팀", "반", "조", "그룹",
					"모임", "동아리", "동기", "멤버", "일행", "패거리",
					"형제", "자매", "남매", "부모", "부모님",
					"친구들", "사람들", "애들", "얘들", "동료들", "팀원들", "가족들"
			);

	/** "한" 은 넣지 않는다. "한영", "한나" 가 깨진다. */
	private static final Set<String> DETERMINERS =
			Set.of("그", "저", "이", "다른", "어떤", "무슨", "웬");

	/** 한 글자 머리 명사("자", "이")는 넣지 않는다. "이모", "이수" 가 깨진다. */
	private static final Set<String> GENERIC_HEADS =
			Set.of(
					"사람", "사람들", "애", "애들", "얘", "얘들",
					"분", "분들", "친구", "친구들",
					"녀석", "녀석들", "여자", "남자", "아이", "아이들"
			);

	private static final Set<String> QUANTITY_WORDS =
			Set.of(
					"한", "두", "세", "네", "다섯", "여섯", "일곱", "여덟", "아홉", "열",
					"둘", "셋", "넷",
					"여러", "몇", "몇몇", "여럿", "수", "수십", "온"
			);

	private static final Set<String> COUNT_UNITS =
			Set.of("명", "분", "사람", "이서", "커플", "쌍");

	private final PersonNormalizer personNormalizer;

	public ExtractionResultValidator(
			PersonNormalizer personNormalizer
	) {
		this.personNormalizer = personNormalizer;
	}

	/*
	 * Gemini/n8n DTO 경계 — entityType은 외부 JSON 원문이라 String을 유지하고,
	 * 비교 대상만 내부 enum 이름에서 가져와 literal 중복을 없앤다.
	 */
	private static final String PERSON =
			MentionEntityType.PERSON.name();
	private static final String PLACE =
			MentionEntityType.PLACE.name();
	private static final String ACTIVITY =
			MentionEntityType.ACTIVITY.name();

	public void validate(
			String diaryContent,
			ExtractionResult extractionResult
	) {
		List<String> errors =
				new ArrayList<>();

		if (diaryContent == null) {
			errors.add(
					"diaryContent must not be null"
			);
		}

		if (extractionResult == null) {
			errors.add(
					"extractionResult must not be null"
			);

			throwIfInvalid(errors);
			return;
		}

		validateArrays(
				extractionResult,
				errors
		);

		if (hasNullArray(extractionResult)) {
			throwIfInvalid(errors);
			return;
		}

		Set<String> personRefs =
				validatePersons(
						diaryContent,
						extractionResult.persons(),
						errors
				);

		validatePlaces(
				diaryContent,
				extractionResult.places(),
				personRefs,
				errors
		);

		validateActivities(
				diaryContent,
				extractionResult.activities(),
				personRefs,
				errors
		);

		validateNoDuplicateSources(
				extractionResult,
				errors
		);

		throwIfInvalid(errors);
	}

	private void validateArrays(
			ExtractionResult extractionResult,
			List<String> errors
	) {
		if (extractionResult.persons() == null) {
			errors.add(
					"persons must not be null"
			);
		}

		if (extractionResult.places() == null) {
			errors.add(
					"places must not be null"
			);
		}

		if (extractionResult.activities() == null) {
			errors.add(
					"activities must not be null"
			);
		}
	}

	private boolean hasNullArray(
			ExtractionResult extractionResult
	) {
		return extractionResult.persons() == null
				|| extractionResult.places() == null
				|| extractionResult.activities() == null;
	}

	private Set<String> validatePersons(
			String diaryContent,
			List<PersonExtraction> persons,
			List<String> errors
	) {
		Set<String> knownPersonRefs =
				new HashSet<>();

		for (int index = 0;
			 index < persons.size();
			 index++) {

			PersonExtraction person =
					persons.get(index);

			String path =
					"persons[" + index + "]";

			if (person == null) {
				errors.add(
						path + " must not be null"
				);
				continue;
			}

			validateEntityType(
					person.entityType(),
					PERSON,
					path,
					errors
			);

			validateRawText(
					diaryContent,
					person.rawText(),
					path,
					errors
			);

			validatePersonNormalization(
					person.rawText(),
					path,
					errors
			);

			String ref =
					person.ref();

			if (isBlank(ref)) {
				errors.add(
						path + ".ref must not be blank"
				);
				continue;
			}

			if (!knownPersonRefs.add(ref)) {
				errors.add(
						"PERSON ref is duplicated: "
								+ ref
				);
			}
		}

		return knownPersonRefs;
	}

	private void validatePlaces(
			String diaryContent,
			List<PlaceExtraction> places,
			Set<String> personRefs,
			List<String> errors
	) {
		for (int index = 0;
			 index < places.size();
			 index++) {

			PlaceExtraction place =
					places.get(index);

			String path =
					"places[" + index + "]";

			if (place == null) {
				errors.add(
						path + " must not be null"
				);
				continue;
			}

			validateEntityType(
					place.entityType(),
					PLACE,
					path,
					errors
			);

			validateRawText(
					diaryContent,
					place.rawText(),
					path,
					errors
			);

			validateNormalizedText(
					place.normalizedText(),
					path,
					errors
			);

			validatePersonRefs(
					place.personRefs(),
					personRefs,
					path,
					errors
			);
		}
	}

	private void validateActivities(
			String diaryContent,
			List<ActivityExtraction> activities,
			Set<String> personRefs,
			List<String> errors
	) {
		for (int index = 0;
			 index < activities.size();
			 index++) {

			ActivityExtraction activity =
					activities.get(index);

			String path =
					"activities[" + index + "]";

			if (activity == null) {
				errors.add(
						path + " must not be null"
				);
				continue;
			}

			validateEntityType(
					activity.entityType(),
					ACTIVITY,
					path,
					errors
			);

			validateRawText(
					diaryContent,
					activity.rawText(),
					path,
					errors
			);

			validateNormalizedText(
					activity.normalizedText(),
					path,
					errors
			);

			validatePersonRefs(
					activity.personRefs(),
					personRefs,
					path,
					errors
			);
		}
	}

	/*
	 * 정규화 결과가 비면 정상 PERSON으로 저장할 수 없다 (명세 8.2-6) — 이 검사가 없으면
	 * MentionCandidateService의 IllegalArgumentException이 터져 Retry 없이 일기 저장 전체가 실패한다.
	 */
	private void validatePersonNormalization(
			String rawText,
			String path,
			List<String> errors
	) {
		if (isBlank(rawText)) {
			return;
		}

		PersonNormalization normalization =
				personNormalizer.normalize(rawText);

		if (isBlank(normalization.normalizedText())) {
			errors.add(
					path
							+ ".rawText cannot be normalized to a person name: "
							+ rawText
			);
			return;
		}

		// "다 함께" 는 "께" 가 조사로 깎여 "다 함" 이 되므로 원문도 본다.
		if (isNonPersonTerm(rawText)
				|| isNonPersonTerm(normalization.normalizedText())
				|| isNonPersonTerm(
				normalization.displayNameCandidate()
		)) {
			errors.add(
					path
							+ ".rawText is not a person: "
							+ rawText
			);
		}
	}

	private boolean isNonPersonTerm(String value) {
		if (isBlank(value)) {
			return false;
		}

		String compact =
				value.replace(" ", "");

		if (compact.isEmpty()) {
			return false;
		}

		return NON_PERSON_TERMS.contains(compact)
				|| isPronounPlural(compact)
				|| isPronounGroup(compact)
				|| isDeterminerGeneric(compact)
				|| isCountExpression(compact);
	}

	/** "우리들", "저희들", "걔네", "우리끼리" */
	private boolean isPronounPlural(String compact) {
		for (String suffix
				: PRONOUN_PLURAL_SUFFIXES) {

			String stem =
					stripSuffix(compact, suffix);

			if (stem == null) {
				continue;
			}

			// "그들끼리" 처럼 이미 복수인 어간도 있다.
			if (PRONOUN_STEMS.contains(stem)
					|| NON_PERSON_TERMS.contains(stem)) {
				return true;
			}
		}

		return false;
	}

	/** "우리 가족", "우리 팀", "내 친구들" */
	private boolean isPronounGroup(String compact) {
		for (String owner : GROUP_OWNERS) {

			String head =
					stripPrefix(compact, owner);

			if (head == null) {
				continue;
			}

			if (GROUP_HEADS.contains(head)
					|| NON_PERSON_TERMS.contains(head)) {
				return true;
			}
		}

		return false;
	}

	/** "그 사람", "다른 친구", "어떤 애" */
	private boolean isDeterminerGeneric(String compact) {
		for (String determiner : DETERMINERS) {

			String head =
					stripPrefix(compact, determiner);

			if (head != null
					&& GENERIC_HEADS.contains(head)) {
				return true;
			}
		}

		return false;
	}

	/** "여러 명", "몇 명", "세 명", "3명", "두 사람" */
	private boolean isCountExpression(String compact) {
		for (String unit : COUNT_UNITS) {

			String quantity =
					stripSuffix(compact, unit);

			if (quantity == null) {
				continue;
			}

			if (QUANTITY_WORDS.contains(quantity)
					|| isAllDigits(quantity)) {
				return true;
			}
		}

		return false;
	}

	/** 나머지가 비면 결합이 아니라 그 단어 자체이므로 null. */
	private String stripPrefix(
			String compact,
			String prefix
	) {
		return compact.length() > prefix.length()
				&& compact.startsWith(prefix)
				? compact.substring(prefix.length())
				: null;
	}

	private String stripSuffix(
			String compact,
			String suffix
	) {
		return compact.length() > suffix.length()
				&& compact.endsWith(suffix)
				? compact.substring(
				0,
				compact.length() - suffix.length()
		)
				: null;
	}

	private boolean isAllDigits(String value) {
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

	/*
	 * 명백히 중복된 PLACE/ACTIVITY는 거부한다 (명세 5.3-9). PERSON은 같은 표현이 여러 ref로
	 * 나뉠 수 있고 Listener가 수렴시키므로 여기서 막지 않는다.
	 */
	private void validateNoDuplicateSources(
			ExtractionResult extractionResult,
			List<String> errors
	) {
		Set<SourceKey> seen = new HashSet<>();

		for (PlaceExtraction place
				: extractionResult.places()) {

			if (place == null) {
				continue;
			}

			addDuplicateError(
					seen,
					PLACE,
					place.rawText(),
					place.normalizedText(),
					place.personRefs(),
					errors
			);
		}

		for (ActivityExtraction activity
				: extractionResult.activities()) {

			if (activity == null) {
				continue;
			}

			addDuplicateError(
					seen,
					ACTIVITY,
					activity.rawText(),
					activity.normalizedText(),
					activity.personRefs(),
					errors
			);
		}
	}

	private void addDuplicateError(
			Set<SourceKey> seen,
			String entityType,
			String rawText,
			String normalizedText,
			List<String> personRefs,
			List<String> errors
	) {
		/*
		 * personRefs에 null이 있으면 validatePersonRefs가 이미 에러를 기록했으므로,
		 * Collections.sort의 NPE로 전체 검증이 죽지 않도록 여기서는 조용히 넘어간다.
		 */
		if (isBlank(rawText)
				|| isBlank(normalizedText)
				|| personRefs == null
				|| personRefs.stream().anyMatch(this::isBlank)) {
			return;
		}

		List<String> sortedRefs =
				new ArrayList<>(personRefs);

		Collections.sort(sortedRefs);

		SourceKey key =
				new SourceKey(
						entityType,
						rawText,
						normalizedText.strip(),
						sortedRefs
				);

		if (!seen.add(key)) {
			errors.add(
					"duplicated "
							+ entityType
							+ " extraction: "
							+ rawText
			);
		}
	}

	private record SourceKey(
			String entityType,
			String rawText,
			String normalizedText,
			List<String> personRefs
	) {
	}

	private void validateRawText(
			String diaryContent,
			String rawText,
			String path,
			List<String> errors
	) {
		if (isBlank(rawText)) {
			errors.add(
					path + ".rawText must not be blank"
			);
			return;
		}

		if (diaryContent == null) {
			return;
		}

		if (!diaryContent.contains(rawText)) {
			errors.add(
					path
							+ ".rawText is not an exact diary substring: "
							+ rawText
			);
		}
	}

	private void validateNormalizedText(
			String normalizedText,
			String path,
			List<String> errors
	) {
		if (isBlank(normalizedText)) {
			errors.add(
					path
							+ ".normalizedText must not be blank"
			);
		}
	}

	private void validateEntityType(
			String actualType,
			String expectedType,
			String path,
			List<String> errors
	) {
		if (!expectedType.equals(actualType)) {
			errors.add(
					path
							+ ".entityType must be "
							+ expectedType
							+ " but was "
							+ actualType
			);
		}
	}

	private void validatePersonRefs(
			List<String> refs,
			Set<String> knownPersonRefs,
			String path,
			List<String> errors
	) {
		if (refs == null) {
			errors.add(
					path
							+ ".personRefs must not be null"
			);
			return;
		}

		Set<String> seenRefs =
				new HashSet<>();

		for (String ref : refs) {
			if (isBlank(ref)) {
				errors.add(
						path
								+ ".personRefs contains blank ref"
				);
				continue;
			}

			if (!seenRefs.add(ref)) {
				errors.add(
						path
								+ ".personRefs contains duplicated ref: "
								+ ref
				);
				continue;
			}

			if (!knownPersonRefs.contains(ref)) {
				errors.add(
						path
								+ ".personRefs contains unknown PERSON ref: "
								+ ref
				);
			}
		}
	}

	private boolean isBlank(
			String value
	) {
		return value == null
				|| value.isBlank();
	}

	private void throwIfInvalid(
			List<String> errors
	) {
		if (!errors.isEmpty()) {
			throw new ExtractionValidationException(
					errors
			);
		}
	}
}