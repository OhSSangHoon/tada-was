package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.PersonAlias;
import com.tada.tada.curator.repository.PersonAliasRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonAliasService {

	private final PersonAliasRepository personAliasRepository;
	private final PersonNormalizer personNormalizer;

	public void saveIfAbsent(
			UUID userId,
			UUID personId,
			String rawAliasText
	) {
		if (userId == null) {
			throw new IllegalArgumentException(
					"userId must not be null"
			);
		}

		if (personId == null) {
			throw new IllegalArgumentException(
					"personId must not be null"
			);
		}

		if (rawAliasText == null) {
			return;
		}

		String aliasText =
				rawAliasText.strip();

		if (aliasText.isBlank()) {
			return;
		}

		boolean exists =
				personAliasRepository
						.existsByOwnerUserIdAndPersonIdAndAliasText(
								userId,
								personId,
								aliasText
						);

		if (exists) {
			return;
		}

		String normalizedText =
				personNormalizer.normalizeName(
						aliasText
				);

		if (normalizedText.isBlank()) {
			return;
		}

		PersonAlias alias =
				PersonAlias.create(
						personId,
						userId,
						aliasText,
						normalizedText
				);

		personAliasRepository.save(
				alias
		);
	}
}
