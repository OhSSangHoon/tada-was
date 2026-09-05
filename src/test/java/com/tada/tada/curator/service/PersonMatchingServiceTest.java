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
	void 원문_exact가_있으면_조사_제거_exact보다_우선한다() {
		UUID userId = UUID.randomUUID();
		UUID rawPersonId = UUID.randomUUID();
		UUID normalizedPersonId = UUID.randomUUID();

		MemoryPerson rawPerson = createMemoryPerson(
				rawPersonId, userId, "민수가"
		);
		MemoryPerson normalizedPerson = createMemoryPerson(
				normalizedPersonId, userId, "민수"
		);

		when(memoryPersonRepository.findAllByUserIdAndDisplayNameIn(
				eq(userId), any()
		)).thenReturn(List.of(rawPerson, normalizedPerson));
		when(personAliasRepository.findAllByOwnerUserIdAndNormalizedTextIn(
				eq(userId), any()
		)).thenReturn(List.of());

		PersonMatchResult result = personMatchingService.match(userId, "민수가");

		assertEquals(PersonMatchType.EXACT, result.matchType());
		assertEquals(rawPersonId, result.matchedPersonId());
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
	void 한_글자_입력에는_편집거리_후보를_만들지_않는다() {
		UUID userId = UUID.randomUUID();
		MemoryPerson person = createMemoryPerson(
				UUID.randomUUID(), userId, "김"
		);

		when(memoryPersonRepository.findAllByUserIdAndDisplayNameIn(
				eq(userId), any()
		)).thenReturn(List.of());
		when(personAliasRepository.findAllByOwnerUserIdAndNormalizedTextIn(
				eq(userId), any()
		)).thenReturn(List.of());
		when(memoryPersonRepository.findAllByUserId(userId))
				.thenReturn(List.of(person));
		when(personAliasRepository.findAllByOwnerUserId(userId))
				.thenReturn(List.of());

		PersonMatchResult result = personMatchingService.match(userId, "이");

		assertEquals(PersonMatchType.NEW, result.matchType());
		assertEquals(List.of(), result.candidatePersonIds());
	}

	@Test
	void 이름_끝일_수_있는_접미사는_기존_인물이_있을_때만_연결한다() {
		/*
		 * "김사랑"은 이름 그 자체일 수도 "김사 + 랑"일 수도 있다.
		 *
		 * 이미 "김사"라는 인물이 있으면 조사 해석이 근거를 얻으므로
		 * 그 인물로 연결한다. 근거가 없으면 새 인물이 되고
		 * 이때 표시 이름은 "김사랑" 원문이 그대로 유지된다.
		 * (PersonNormalizerTest 참고)
		 *
		 * 은/이 접미사와 동일한 정책이다.
		 */
		UUID userId = UUID.randomUUID();
		UUID shortLoveId = UUID.randomUUID();

		MemoryPerson shortLove = createMemoryPerson(
				shortLoveId,
				userId,
				"김사"
		);

		when(memoryPersonRepository.findAllByUserIdAndDisplayNameIn(
				eq(userId),
				any()
		)).thenReturn(List.of(shortLove));

		when(personAliasRepository.findAllByOwnerUserIdAndNormalizedTextIn(
				eq(userId),
				any()
		)).thenReturn(List.of());

		PersonMatchResult result =
				personMatchingService.match(userId, "김사랑");

		assertEquals(
				PersonMatchType.EXACT,
				result.matchType()
		);

		assertEquals(
				shortLoveId,
				result.matchedPersonId()
		);
	}

	@Test
	void 이름_끝일_수_있는_접미사만으로는_없는_인물을_만들어내지_않는다() {
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

		assertEquals(
				PersonMatchType.NEW,
				personMatchingService.match(userId, "김사랑").matchType()
		);

		assertEquals(
				PersonMatchType.NEW,
				personMatchingService.match(userId, "이영도").matchType()
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
	void 주격_조사가_붙은_기존_인물은_exact로_반환한다() {
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
		)).thenReturn(List.of(person));

		when(personAliasRepository.findAllByOwnerUserIdAndNormalizedTextIn(
				eq(userId),
				any()
		)).thenReturn(List.of());

		PersonMatchResult result =
				personMatchingService.match(
						userId,
						"김민혁이"
				);

		assertEquals(
				PersonMatchType.EXACT,
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

	@Test
	void 정규화하면_같은_이름으로_수렴하는_기존_인물을_exact로_찾는다() {
		/*
		 * 신규 인물의 displayName 은 이름 훼손을 막기 위해
		 * "은", "이" 같은 애매한 접미사를 보존한다. ("가을이")
		 *
		 * 같은 사람이 나중에 다른 조사로 등장하면
		 * displayName 직접 비교로는 찾을 수 없으므로
		 * 조회 시점 정규화로 다시 연결한다.
		 */
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		MemoryPerson person = createMemoryPerson(
				personId,
				userId,
				"가을이"
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
						"가을이가"
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
	void 정규화하면_같은_이름이_되는_인물이_여러_명이면_ambiguous를_반환한다() {
		UUID userId = UUID.randomUUID();
		UUID personId1 = UUID.randomUUID();
		UUID personId2 = UUID.randomUUID();

		MemoryPerson person1 = createMemoryPerson(
				personId1,
				userId,
				"한영은"
		);

		MemoryPerson person2 = createMemoryPerson(
				personId2,
				userId,
				"한영이"
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
				.thenReturn(List.of(person1, person2));

		when(personAliasRepository.findAllByOwnerUserId(userId))
				.thenReturn(List.of());

		PersonMatchResult result =
				personMatchingService.match(
						userId,
						"한영이가"
				);

		assertEquals(
				PersonMatchType.AMBIGUOUS,
				result.matchType()
		);

		assertNull(result.matchedPersonId());
	}

	@Test
	void 원문_exact가_정규화_수렴_후보보다_우선한다() {
		/*
		 * displayName 직접 일치가 존재하면
		 * 정규화하면 같아지는 다른 인물 때문에
		 * ambiguous 로 내려가지 않는다.
		 */
		UUID userId = UUID.randomUUID();
		UUID directPersonId = UUID.randomUUID();
		UUID normalizedPersonId = UUID.randomUUID();

		MemoryPerson directPerson = createMemoryPerson(
				directPersonId,
				userId,
				"한영"
		);

		MemoryPerson normalizedPerson = createMemoryPerson(
				normalizedPersonId,
				userId,
				"한영은"
		);

		when(memoryPersonRepository.findAllByUserIdAndDisplayNameIn(
				eq(userId),
				any()
		)).thenReturn(List.of(directPerson));

		when(personAliasRepository.findAllByOwnerUserIdAndNormalizedTextIn(
				eq(userId),
				any()
		)).thenReturn(List.of());

		when(memoryPersonRepository.findAllByUserId(userId))
				.thenReturn(List.of(directPerson, normalizedPerson));

		when(personAliasRepository.findAllByOwnerUserId(userId))
				.thenReturn(List.of());

		PersonMatchResult result =
				personMatchingService.match(
						userId,
						"한영이가"
				);

		assertEquals(
				PersonMatchType.EXACT,
				result.matchType()
		);

		assertEquals(
				directPersonId,
				result.matchedPersonId()
		);
	}

	@Test
	void 정규화_수렴_인물도_block되면_자동_연결하지_않는다() {
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		MemoryPerson person = createMemoryPerson(
				personId,
				userId,
				"가을이"
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
						"가을이가",
						Set.of(personId)
				);

		assertEquals(
				PersonMatchType.NEW,
				result.matchType()
		);
	}
}
