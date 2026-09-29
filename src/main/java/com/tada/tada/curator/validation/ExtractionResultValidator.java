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

		if (isBlank(
				normalization.normalizedText()
		)) {
			errors.add(
					path
							+ ".rawText cannot be normalized to a person name: "
							+ rawText
			);
		}
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
