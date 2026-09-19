package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.PersonAlias;
import com.tada.tada.curator.repository.PersonAliasRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonAliasServiceTest {

	private PersonAliasRepository personAliasRepository;
	private PersonNormalizer personNormalizer;
	private PersonAliasService personAliasService;

	@BeforeEach
	void setUp() {
		personAliasRepository =
				Mockito.mock(
						PersonAliasRepository.class
				);

		personNormalizer =
				Mockito.mock(
						PersonNormalizer.class
				);

		personAliasService =
				new PersonAliasService(
						personAliasRepository,
						personNormalizer
				);
	}

	@Test
	void Alias가_없으면_정규화해서_저장한다() {
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		when(
				personAliasRepository
						.existsByOwnerUserIdAndPersonIdAndAliasText(
								userId,
								personId,
								"민수랑"
						)
		).thenReturn(false);

		when(
				personNormalizer.normalizeName(
						"민수랑"
				)
		).thenReturn("민수");

		personAliasService.saveIfAbsent(
				userId,
				personId,
				" 민수랑 "
		);

		ArgumentCaptor<PersonAlias> aliasCaptor =
				ArgumentCaptor.forClass(
						PersonAlias.class
				);

		verify(
				personAliasRepository
		).save(
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
				"민수랑",
				savedAlias.getAliasText()
		);

		assertEquals(
				"민수",
				savedAlias.getNormalizedText()
		);
	}

	@Test
	void 같은_Alias가_이미_있으면_저장하지_않는다() {
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		when(
				personAliasRepository
						.existsByOwnerUserIdAndPersonIdAndAliasText(
								userId,
								personId,
								"민수"
						)
		).thenReturn(true);

		personAliasService.saveIfAbsent(
				userId,
				personId,
				"민수"
		);

		verify(
				personAliasRepository,
				never()
		).save(
				Mockito.any(PersonAlias.class)
		);
	}

	@Test
	void 공백_Alias는_저장하지_않는다() {
		personAliasService.saveIfAbsent(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"   "
		);

		verify(
				personAliasRepository,
				never()
		).save(
				Mockito.any(PersonAlias.class)
		);
	}

	@Test
	void null_Alias는_저장하지_않는다() {
		personAliasService.saveIfAbsent(
				UUID.randomUUID(),
				UUID.randomUUID(),
				null
		);

		verify(
				personAliasRepository,
				never()
		).save(
				Mockito.any(PersonAlias.class)
		);
	}

	@Test
	void 정규화_결과가_비면_저장하지_않는다() {
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		when(
				personAliasRepository
						.existsByOwnerUserIdAndPersonIdAndAliasText(
								userId,
								personId,
								"테스트"
						)
		).thenReturn(false);

		when(
				personNormalizer.normalizeName(
						"테스트"
				)
		).thenReturn("");

		personAliasService.saveIfAbsent(
				userId,
				personId,
				"테스트"
		);

		verify(
				personAliasRepository,
				never()
		).save(
				Mockito.any(PersonAlias.class)
		);
	}
}
