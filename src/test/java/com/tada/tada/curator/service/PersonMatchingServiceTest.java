package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.MemoryPerson;
import com.tada.tada.curator.entity.PersonAlias;
import com.tada.tada.curator.model.PersonMatchResult;
import com.tada.tada.curator.model.PersonMatchType;
import com.tada.tada.curator.repository.MemoryPersonRepository;
import com.tada.tada.curator.repository.PersonAliasRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class PersonMatchingServiceTest {

	private MemoryPersonRepository memoryPersonRepository;
	private PersonAliasRepository personAliasRepository;
	private PersonMatchingService personMatchingService;

	@BeforeEach
	void setUp() {
		memoryPersonRepository =
				Mockito.mock(MemoryPersonRepository.class);

		personAliasRepository =
				Mockito.mock(PersonAliasRepository.class);

		personMatchingService = new PersonMatchingService(
				new PersonNormalizer(),
				memoryPersonRepository,
				personAliasRepository
		);
	}

	@Test
	void 기존_인물_한명과_정확히_일치하면_exact를_반환한다() {
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		MemoryPerson person = createMemoryPerson(
				personId,
				userId,
				"한영"
		);

		when(memoryPersonRepository.findAllByUserIdAndDisplayNameIn(
				eq(userId),
				any()
		)).thenReturn(List.of(person));

		when(personAliasRepository.findAllByOwnerUserIdAndNormalizedTextIn(
				eq(userId),
				any()
		)).thenReturn(List.of());

		PersonMatchResult result =
				personMatchingService.match(
						userId,
						"한영이와"
				);

		assertEquals(
				PersonMatchType.EXACT,
				result.matchType()
		);

		assertEquals(
				personId,
				result.matchedPersonId()
		);
	}

	@Test
	void 같은_이름의_인물이_두명이면_ambiguous를_반환한다() {
		UUID userId = UUID.randomUUID();

		UUID personId1 = UUID.randomUUID();
		UUID personId2 = UUID.randomUUID();

		MemoryPerson person1 = createMemoryPerson(
				personId1,
				userId,
				"민수"
		);

		MemoryPerson person2 = createMemoryPerson(
				personId2,
				userId,
				"민수"
		);

		when(memoryPersonRepository.findAllByUserIdAndDisplayNameIn(
				eq(userId),
				any()
		)).thenReturn(
				List.of(
						person1,
						person2
				)
		);

		when(personAliasRepository.findAllByOwnerUserIdAndNormalizedTextIn(
				eq(userId),
				any()
		)).thenReturn(List.of());

		PersonMatchResult result =
				personMatchingService.match(
						userId,
						"민수와"
				);

		assertEquals(
				PersonMatchType.AMBIGUOUS,
				result.matchType()
		);

		assertNull(
				result.matchedPersonId()
		);

		assertEquals(
				2,
				result.candidatePersonIds().size()
		);
	}

	@Test
	void memoryPerson과_alias가_같은_personId면_한명으로_처리한다() {
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		MemoryPerson person = createMemoryPerson(
				personId,
				userId,
				"김민수"
		);

		PersonAlias alias = createPersonAlias(
				UUID.randomUUID(),
				personId,
				userId,
				"민수",
				"민수"
		);

		when(memoryPersonRepository.findAllByUserIdAndDisplayNameIn(
				eq(userId),
				any()
		)).thenReturn(List.of(person));

		when(personAliasRepository.findAllByOwnerUserIdAndNormalizedTextIn(
				eq(userId),
				any()
		)).thenReturn(List.of(alias));

		PersonMatchResult result =
				personMatchingService.match(
						userId,
						"민수"
				);

		assertEquals(
				PersonMatchType.EXACT,
				result.matchType()
		);

		assertEquals(
				personId,
				result.matchedPersonId()
		);
	}

	@Test
	void 짧은_이름의_한글자_차이만으로는_similar로_자동_연결하지_않는다() {
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		MemoryPerson person = createMemoryPerson(
				personId,
				userId,
				"민수"
		);

		when(memoryPersonRepository.findAllByUserIdAndDisplayNameIn(
				eq(userId),
				any()
		)).thenReturn(List.of());

		when(personAliasRepository.findAllByOwnerUserIdAndNormalizedTextIn(
				eq(userId),
				any()
		)).thenReturn(List.of());

		when(memoryPersonRepository.findAllByUserId(userId))
				.thenReturn(List.of(person));

		when(personAliasRepository.findAllByOwnerUserId(userId))
				.thenReturn(List.of());

		PersonMatchResult result =
				personMatchingService.match(
						userId,
						"민서"
				);

		assertEquals(
				PersonMatchType.AMBIGUOUS,
				result.matchType()
		);

		assertNull(
				result.matchedPersonId()
		);

		assertEquals(
				List.of(personId),
				result.candidatePersonIds()
		);
	}

	@Test
	void 정확하거나_비슷한_인물이_없으면_new를_반환한다() {
		UUID userId = UUID.randomUUID();

		when(memoryPersonRepository.findAllByUserIdAndDisplayNameIn(
				eq(userId),
				any()
		)).thenReturn(List.of());

		when(personAliasRepository.findAllByOwnerUserIdAndNormalizedTextIn(
				eq(userId),
				any()
		)).thenReturn(List.of());

		when(memoryPersonRepository.findAllByUserId(userId))
				.thenReturn(List.of());

		when(personAliasRepository.findAllByOwnerUserId(userId))
				.thenReturn(List.of());

		PersonMatchResult result =
				personMatchingService.match(
						userId,
						"철웅"
				);

		assertEquals(
				PersonMatchType.NEW,
				result.matchType()
		);

		assertNull(
				result.matchedPersonId()
		);

		assertEquals(
				List.of(),
				result.candidatePersonIds()
		);
	}

	private MemoryPerson createMemoryPerson(
			UUID id,
			UUID userId,
			String displayName
	) {
		MemoryPerson person =
				newInstance(MemoryPerson.class);

		setField(
				person,
				"id",
				id
		);

		setField(
				person,
				"userId",
				userId
		);

		setField(
				person,
				"displayName",
				displayName
		);

		setField(
				person,
				"createdAt",
				LocalDateTime.now()
		);

		return person;
	}

	private PersonAlias createPersonAlias(
			UUID id,
			UUID personId,
			UUID ownerUserId,
			String aliasText,
			String normalizedText
	) {
		PersonAlias alias =
				newInstance(PersonAlias.class);

		setField(
				alias,
				"id",
				id
		);

		setField(
				alias,
				"personId",
				personId
		);

		setField(
				alias,
				"ownerUserId",
				ownerUserId
		);

		setField(
				alias,
				"aliasText",
				aliasText
		);

		setField(
				alias,
				"normalizedText",
				normalizedText
		);

		return alias;
	}

	private <T> T newInstance(Class<T> clazz) {
		try {
			var constructor =
					clazz.getDeclaredConstructor();

			constructor.setAccessible(true);

			return constructor.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void setField(
			Object target,
			String fieldName,
			Object value
	) {
		try {
			Field field =
					target
							.getClass()
							.getDeclaredField(fieldName);

			field.setAccessible(true);
			field.set(target, value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void 긴_이름의_한글자_차이_하나만으로는_similar가_되지_않는다() {
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		MemoryPerson person = createMemoryPerson(
				personId,
				userId,
				"김민혁"
		);

		when(memoryPersonRepository.findAllByUserIdAndDisplayNameIn(
				eq(userId),
				any()
		)).thenReturn(List.of());

		when(personAliasRepository.findAllByOwnerUserIdAndNormalizedTextIn(
				eq(userId),
				any()
		)).thenReturn(List.of());

		when(memoryPersonRepository.findAllByUserId(userId))
				.thenReturn(List.of(person));

		when(personAliasRepository.findAllByOwnerUserId(userId))
				.thenReturn(List.of());

		PersonMatchResult result =
				personMatchingService.match(
						userId,
						"김민헉"
				);

		assertEquals(
				PersonMatchType.AMBIGUOUS,
				result.matchType()
		);

		assertNull(result.matchedPersonId());

		assertEquals(
				List.of(personId),
				result.candidatePersonIds()
		);
	}

	@Test
	void 여러_강한_근거가_같은_인물로_수렴하면_similar를_반환한다() {
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		MemoryPerson person = createMemoryPerson(
				personId,
				userId,
				"김민혁"
		);

		when(memoryPersonRepository.findAllByUserIdAndDisplayNameIn(
				eq(userId),
				any()
		)).thenReturn(List.of());

		when(personAliasRepository.findAllByOwnerUserIdAndNormalizedTextIn(
				eq(userId),
				any()
		)).thenReturn(List.of());

		when(memoryPersonRepository.findAllByUserId(userId))
				.thenReturn(List.of(person));

		when(personAliasRepository.findAllByOwnerUserId(userId))
				.thenReturn(List.of());

		PersonMatchResult result =
				personMatchingService.match(
						userId,
						"김민혁이"
				);

		assertEquals(
				PersonMatchType.SIMILAR,
				result.matchType()
		);

		assertEquals(
				personId,
				result.matchedPersonId()
		);

		assertEquals(
				List.of(),
				result.candidatePersonIds()
		);
	}

	@Test
	void block된_인물은_exact여도_자동_연결하지_않는다() {
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		MemoryPerson person = createMemoryPerson(
				personId,
				userId,
				"민수"
		);

		when(memoryPersonRepository.findAllByUserIdAndDisplayNameIn(
				eq(userId),
				any()
		)).thenReturn(List.of(person));

		when(personAliasRepository.findAllByOwnerUserIdAndNormalizedTextIn(
				eq(userId),
				any()
		)).thenReturn(List.of());

		when(memoryPersonRepository.findAllByUserId(userId))
				.thenReturn(List.of(person));

		when(personAliasRepository.findAllByOwnerUserId(userId))
				.thenReturn(List.of());

		PersonMatchResult result =
				personMatchingService.match(
						userId,
						"민수",
						Set.of(personId)
				);

		assertEquals(
				PersonMatchType.NEW,
				result.matchType()
		);

		assertNull(result.matchedPersonId());
	}
}