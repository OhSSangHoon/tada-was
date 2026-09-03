package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.MemoryPerson;
import com.tada.tada.curator.model.PersonMatchResult;
import com.tada.tada.curator.repository.MemoryPersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonResolverServiceTest {

	private PersonMatchingService personMatchingService;
	private PersonCreationGuard personCreationGuard;
	private MemoryPersonRepository memoryPersonRepository;

	private PersonResolverService personResolverService;

	@BeforeEach
	void setUp() {
		personMatchingService =
				Mockito.mock(PersonMatchingService.class);

		personCreationGuard =
				Mockito.mock(PersonCreationGuard.class);

		memoryPersonRepository =
				Mockito.mock(MemoryPersonRepository.class);

		personResolverService =
				new PersonResolverService(
						personMatchingService,
						personCreationGuard,
						new PersonNormalizer(),
						memoryPersonRepository
				);
	}

	@Test
	void exact이면_기존_인물을_그대로_사용한다() {
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		when(personMatchingService.match(
				userId,
				"민수",
				Set.of()
		)).thenReturn(
				PersonMatchResult.exact(personId)
		);

		UUID result =
				personResolverService.resolve(
						userId,
						"민수"
				);

		assertEquals(
				personId,
				result
		);

		verify(
				personCreationGuard,
				never()
		).findReusablePerson(
				any(),
				any(),
				any(),
				any()
		);

		verify(
				memoryPersonRepository,
				never()
		).save(any());
	}

	@Test
	void similar이면_기존_인물을_자동_연결한다() {
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		when(personMatchingService.match(
				userId,
				"김민혁이",
				Set.of()
		)).thenReturn(
				PersonMatchResult.similar(personId)
		);

		UUID result =
				personResolverService.resolve(
						userId,
						"김민혁이"
				);

		assertEquals(
				personId,
				result
		);

		verify(
				personCreationGuard,
				never()
		).findReusablePerson(
				any(),
				any(),
				any(),
				any()
		);

		verify(
				memoryPersonRepository,
				never()
		).save(any());
	}

	@Test
	void ambiguous라도_creationGuard에_안정적인_이력이_있으면_기존_인물을_재사용한다() {
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		MemoryPerson person =
				MemoryPerson.create(
						userId,
						"민혁상"
				);

		when(personMatchingService.match(
				userId,
				"민혁상",
				Set.of()
		)).thenReturn(
				PersonMatchResult.ambiguous(
						java.util.List.of(
								UUID.randomUUID()
						)
				)
		);

		when(personCreationGuard.findReusablePerson(
				userId,
				"민혁상",
				"민혁상",
				Set.of()
		)).thenReturn(
				Optional.of(personId)
		);

		when(memoryPersonRepository.findById(personId))
				.thenReturn(
						Optional.of(person)
				);

		UUID result =
				personResolverService.resolve(
						userId,
						"민혁상"
				);

		assertEquals(
				person.getId(),
				result
		);

		verify(
				memoryPersonRepository,
				never()
		).save(any());
	}

	@Test
	void new이고_creationGuard에서도_재사용할_인물이_없으면_새_인물을_생성한다() {
		UUID userId = UUID.randomUUID();

		when(personMatchingService.match(
				userId,
				"철웅",
				Set.of()
		)).thenReturn(
				PersonMatchResult.newPerson()
		);

		when(personCreationGuard.findReusablePerson(
				userId,
				"철웅",
				"철웅",
				Set.of()
		)).thenReturn(
				Optional.empty()
		);

		when(memoryPersonRepository.save(any()))
				.thenAnswer(
						invocation ->
								invocation.getArgument(0)
				);

		UUID result =
				personResolverService.resolve(
						userId,
						"철웅"
				);

		assertNotNull(result);

		verify(
				memoryPersonRepository
		).save(
				any(MemoryPerson.class)
		);
	}

	@Test
	void 신규_인물은_약한_정규화_후보가_아닌_안전한_normalizedText를_displayName으로_사용한다() {
		UUID userId = UUID.randomUUID();

		when(personMatchingService.match(
				userId,
				"민혁이",
				Set.of()
		)).thenReturn(
				PersonMatchResult.newPerson()
		);

		when(personCreationGuard.findReusablePerson(
				userId,
				"민혁이",
				"민혁이",
				Set.of()
		)).thenReturn(
				Optional.empty()
		);

		when(memoryPersonRepository.save(any()))
				.thenAnswer(invocation -> {
					MemoryPerson person =
							invocation.getArgument(0);

					assertEquals(
							"민혁이",
							person.getDisplayName()
					);

					assertEquals(
							userId,
							person.getUserId()
					);

					return person;
				});

		UUID result =
				personResolverService.resolve(
						userId,
						"민혁이"
				);

		assertNotNull(result);
	}

	@Test
	void creationGuard가_다른_사용자의_인물을_반환하면_거부한다() {
		UUID userId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		MemoryPerson otherPerson =
				MemoryPerson.create(
						otherUserId,
						"민혁상"
				);

		when(personMatchingService.match(
				userId,
				"민혁상",
				Set.of()
		)).thenReturn(
				PersonMatchResult.newPerson()
		);

		when(personCreationGuard.findReusablePerson(
				userId,
				"민혁상",
				"민혁상",
				Set.of()
		)).thenReturn(
				Optional.of(personId)
		);

		when(memoryPersonRepository.findById(personId))
				.thenReturn(
						Optional.of(otherPerson)
				);

		assertThrows(
				IllegalStateException.class,
				() ->
						personResolverService.resolve(
								userId,
								"민혁상"
						)
		);
	}

	@Test
	void block된_인물이_matching결과로_들어와도_최종_연결을_거부한다() {
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		Set<UUID> blockedPersonIds =
				Set.of(personId);

		when(personMatchingService.match(
				userId,
				"민수",
				blockedPersonIds
		)).thenReturn(
				PersonMatchResult.exact(personId)
		);

		assertThrows(
				IllegalStateException.class,
				() ->
						personResolverService.resolve(
								userId,
								"민수",
								blockedPersonIds
						)
		);
	}
}