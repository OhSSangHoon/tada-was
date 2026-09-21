package com.tada.tada.curator.service;

import com.tada.tada.curator.dto.PersonRenameForm;
import com.tada.tada.curator.entity.MemoryPerson;
import com.tada.tada.curator.entity.PersonAlias;
import com.tada.tada.curator.repository.MemoryPersonRepository;
import com.tada.tada.curator.repository.PersonAliasRepository;
import com.tada.tada.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonRenameService {

	private final MemoryPersonRepository memoryPersonRepository;
	private final PersonAliasRepository personAliasRepository;
	private final PersonNormalizer personNormalizer;

	@Transactional
	public void renamePerson(
			UUID userId,
			UUID personId,
			PersonRenameForm form
	) {
		if (userId == null
				|| personId == null
				|| form == null
				|| form.getDisplayName() == null
				|| form.getDisplayName().isBlank()) {

			throw new CustomException(
					"변경할 이름을 입력해주세요.",
					400
			);
		}

		MemoryPerson person =
				memoryPersonRepository
						.findByIdForUpdate(personId)
						.orElseThrow(
								() -> new CustomException(
										"인물을 찾을 수 없습니다.",
										404
								)
						);

		if (!userId.equals(person.getUserId())) {
			throw new CustomException(
					"인물을 찾을 수 없습니다.",
					404
			);
		}

		String newDisplayName =
				form.getDisplayName().strip();

		String oldDisplayName =
				person.getDisplayName();

		if (oldDisplayName.equals(newDisplayName)) {
			return;
		}

		saveOldDisplayNameAsAlias(
				userId,
				personId,
				oldDisplayName
		);

		person.updateDisplayName(
				newDisplayName
		);
	}

	private void saveOldDisplayNameAsAlias(
			UUID userId,
			UUID personId,
			String oldDisplayName
	) {
		String aliasText =
				oldDisplayName.strip();

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

		personAliasRepository.save(alias);
	}
}