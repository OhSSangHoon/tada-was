package com.tada.tada.curator.validation;

import com.tada.tada.curator.exception.ExtractionValidationException;
import com.tada.tada.global.event.dto.ActivityExtraction;
import com.tada.tada.global.event.dto.ExtractionResult;
import com.tada.tada.global.event.dto.PersonExtraction;
import com.tada.tada.global.event.dto.PlaceExtraction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ExtractionResultValidator {

	private static final String PERSON = "PERSON";
	private static final String PLACE = "PLACE";
	private static final String ACTIVITY = "ACTIVITY";

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