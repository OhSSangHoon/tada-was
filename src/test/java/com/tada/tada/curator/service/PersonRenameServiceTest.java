package com.tada.tada.curator.service;

import com.tada.tada.curator.dto.PersonRenameForm;
import com.tada.tada.curator.entity.MemoryPerson;
import com.tada.tada.curator.entity.PersonAlias;
import com.tada.tada.curator.repository.MemoryPersonRepository;
import com.tada.tada.curator.repository.PersonAliasRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonRenameServiceTest {

	private MemoryPersonRepository memoryPersonRepository;
	private PersonAliasRepository personAliasRepository;
	private PersonNormalizer personNormalizer;

	private PersonRenameService personRenameService;

	@BeforeEach
	void setUp() {
		memoryPersonRepository =
				Mockito.mock(MemoryPersonRepository.class);

		personAliasRepository =
				Mockito.mock(PersonAliasRepository.class);

		personNormalizer =
				Mockito.mock(PersonNormalizer.class);

		personRenameService =
				new PersonRenameService(
						memoryPersonRepository,
						personAliasRepository,
						personNormalizer
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

		when(
				personAliasRepository
						.existsByOwnerUserIdAndPersonIdAndAliasText(
								userId,
								personId,
								"엄마도"
						)
		).thenReturn(false);

		when(
				personNormalizer
						.normalizeName("엄마도")
		).thenReturn("엄마");

		personRenameService.renamePerson(
				userId,
				personId,
				form
		);

		assertEquals(
				"엄마",
				person.getDisplayName()
		);

		ArgumentCaptor<PersonAlias> aliasCaptor =
				ArgumentCaptor.forClass(
						PersonAlias.class
				);

		verify(personAliasRepository)
				.save(
						aliasCaptor.capture()
				);

		PersonAlias savedAlias =
				aliasCaptor.getValue();

		assertEquals(
				personId,
				savedAlias.getPersonId()
		);

		assertEquals(
				userId,
				savedAlias.getOwnerUserId()
		);

		assertEquals(
				"엄마도",
				savedAlias.getAliasText()
		);

		assertEquals(
				"엄마",
				savedAlias.getNormalizedText()
		);

		verify(memoryPersonRepository)
				.findByIdForUpdate(personId);
	}
}