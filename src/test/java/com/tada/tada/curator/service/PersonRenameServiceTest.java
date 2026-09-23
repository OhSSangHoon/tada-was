package com.tada.tada.curator.service;

import com.tada.tada.curator.dto.PersonRenameForm;
import com.tada.tada.curator.entity.MemoryPerson;
import com.tada.tada.curator.repository.MemoryPersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonRenameServiceTest {

	private MemoryPersonRepository memoryPersonRepository;
	private PersonAliasService personAliasService;

	private PersonRenameService personRenameService;

	@BeforeEach
	void setUp() {
		memoryPersonRepository =
				Mockito.mock(MemoryPersonRepository.class);

		personAliasService =
				Mockito.mock(
						PersonAliasService.class
				);

		personRenameService =
				new PersonRenameService(
						memoryPersonRepository,
						personAliasService
				);
	}

	@Test
	void 이름을_수정하면_기존_이름을_Alias로_저장하고_displayName을_변경한다() {
		UUID userId = UUID.randomUUID();

		MemoryPerson person =
				MemoryPerson.create(
						userId,
						"엄마도"
				);

		UUID personId =
				person.getId();

		PersonRenameForm form =
				new PersonRenameForm();

		form.setDisplayName("엄마");

		when(
				memoryPersonRepository
						.findByIdForUpdate(personId)
		).thenReturn(
				Optional.of(person)
		);

		personRenameService.renamePerson(
				userId,
				personId,
				form
		);

		assertEquals(
				"엄마",
				person.getDisplayName()
		);

		verify(
				personAliasService
		).saveIfAbsent(
				userId,
				personId,
				"엄마도"
		);

		verify(memoryPersonRepository)
				.findByIdForUpdate(personId);
	}
}