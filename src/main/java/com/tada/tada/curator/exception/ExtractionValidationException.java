package com.tada.tada.curator.exception;

import java.util.List;

public class ExtractionValidationException
		extends RuntimeException {

	private final List<String> validationErrors;

	public ExtractionValidationException(
			List<String> validationErrors
	) {
		super(buildMessage(validationErrors));

		this.validationErrors =
				validationErrors == null
						? List.of()
						: List.copyOf(validationErrors);
	}

	public List<String> getValidationErrors() {
		return validationErrors;
	}

	private static String buildMessage(
			List<String> validationErrors
	) {
		if (validationErrors == null
				|| validationErrors.isEmpty()) {
			return "ExtractionResult validation failed";
		}

		return "ExtractionResult validation failed: "
				+ String.join(
				", ",
				validationErrors
		);
	}
}