package com.tada.tada.curator.service;

import com.tada.tada.curator.dto.PersonRenameForm;
import com.tada.tada.curator.entity.MemoryPerson;
import com.tada.tada.curator.repository.MemoryPersonRepository;
import com.tada.tada.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonRenameService {

	private final MemoryPersonRepository memoryPersonRepository;
	private final PersonAliasService personAliasService;

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

		personAliasService.saveIfAbsent(
				userId,
				personId,
				oldDisplayName
		);

		person.updateDisplayName(
				newDisplayName
		);
	}
}
