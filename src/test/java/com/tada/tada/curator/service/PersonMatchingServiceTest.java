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
	void 애매한_조사를_떼야만_일치하는_기존_인물은_ambiguous_후보로만_올라간다() {
		/*
		 * "한영이와"는 안전 조사 "와"를 떼면 "한영이"까지만 안전하고,
		 * "한영"에 닿으려면 애매한 조사 "이"까지 떼야 한다.
		 *
		 * "한영이"가 실제로는 다른 사람의 이름일 수 있으므로
		 * (감사 사례: 기존 "김성", 입력 "김성은" → 예전에는 EXACT였지만
		 * 실제 이름이 김성은인 사람에게 잘못 붙을 위험이 있었다)
		 * 애매한 조사 제거로만 도달하는 일치는 EXACT로 자동 연결하지
		 * 않고 ambiguous 후보로만 남긴다.
		 */
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

		when(memoryPersonRepository.findAllByUserId(userId))
				.thenReturn(List.of(person));

		when(personAliasRepository.findAllByOwnerUserId(userId))
				.thenReturn(List.of());

		PersonMatchResult result =
				personMatchingService.match(
						userId,
						"한영이와"
				);

		assertEquals(
				PersonMatchType.AMBIGUOUS,
				result.matchType()
		);

		assertEquals(
				List.of(personId),
				result.candidatePersonIds()
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
	void 이름_끝일_수_있는_접미사는_기존_인물이_있어도_ambiguous_후보로만_연결한다() {
		/*
		 * "김사랑"은 이름 그 자체일 수도 "김사 + 랑"일 수도 있다.
		 *
		 * 예전에는 "김사"라는 인물이 있으면 그 조사 해석을 그대로
		 * EXACT로 확정했다. 하지만 실제 이름이 "김사랑"인 사람도
		 * 있을 수 있어, "김사"라는 인물의 존재만으로 자동 연결하면
		 * 서로 다른 두 사람이 합쳐질 위험이 있다. (감사 지적)
		 *
		 * 지금은 "김사"를 ambiguous 후보로만 올리고, 자동 연결은
		 * 다른 근거(반복 언급 이력 등)가 쌓였을 때만 이뤄진다.
		 * 근거가 아예 없으면 새 인물이 되고 표시 이름은 "김사랑"
		 * 원문이 그대로 유지된다. (PersonNormalizerTest 참고)
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

		when(memoryPersonRepository.findAllByUserId(userId))
				.thenReturn(List.of(shortLove));

		when(personAliasRepository.findAllByOwnerUserId(userId))
				.thenReturn(List.of());

		PersonMatchResult result =
				personMatchingService.match(userId, "김사랑");

		assertEquals(
				PersonMatchType.AMBIGUOUS,
				result.matchType()
		);

		assertEquals(
				List.of(shortLoveId),
				result.candidatePersonIds()
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
	void 주격_조사가_붙은_기존_인물은_ambiguous_후보로만_반환한다() {
		/*
		 * "김민혁이"의 "이"는 거의 확실히 주격 조사이지만,
		 * 기계적으로는 "성은/사랑"처럼 이름의 일부일 수도 있는
		 * 애매한 접미사와 같은 갈래다. (감사 지적: 은/이/도/랑/님/씨/아
		 * 제거만으로 EXACT를 만들지 않기)
		 *
		 * "거의 확실히 조사"라는 확신은 아직 코드가 가진 근거가
		 * 아니므로, 지금은 이 경우도 ambiguous 후보로만 올리고
		 * 자동 연결은 반복 언급 이력 등 다른 근거에 맡긴다.
		 */
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
				PersonMatchType.AMBIGUOUS,
				result.matchType()
		);

		assertEquals(
				List.of(personId),
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
	void 안전_조사로_일치하는_기존_인물이_있으면_애매한_수렴_후보를_무시하고_exact로_찾는다() {
		/*
		 * "한영이가"는 안전 조사 "가"만 떼면 "한영이"이고,
		 * 이는 실제로 등록된 인물 "한영이"의 원문과 그대로 같다.
		 * 안전한 근거만으로 이미 유일하게 설명되므로 여기서 멈춘다.
		 *
		 * "한영이"를 다시 애매한 조사로 "한영"까지 떼서 "한영은"과
		 * 충돌시키는 건, 이미 안전하게 설명된 형태를 다시 애매하게
		 * 재해석하는 불필요한 추가 추측이다. 안전한 설명이 있을 때는
		 * 그쪽을 우선한다.
		 */
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
				PersonMatchType.EXACT,
				result.matchType()
		);

		assertEquals(
				personId2,
				result.matchedPersonId()
		);
	}

	@Test
	void 애매한_조사_제거로만_기존_인물과_같아져도_exact로_합치지_않는다() {
		/*
		 * 감사에서 실제로 재현했던 사례(기존 "김성", 입력 "김성은"
		 * → 예전에는 "은"을 애매한 조사로 떼서 EXACT로 합쳤다. 실제
		 * 이름이 "김성은"인 사람일 수도 있어 위험한 연결이었다)의
		 * 변형이다.
		 *
		 * 여기서는 기존 "김성은"에 입력 "김성이"를 준다. 둘 다 애매한
		 * 조사를 떼면 "김성"으로 수렴하지만, 그 수렴형은
		 * safeMatchCandidates에 들어가지 않으므로 EXACT/정규화-EXACT
		 * 어느 단계에서도 자동으로 합쳐지지 않는다. 편집 거리 기반
		 * 약한 점수만 붙어 SIMILAR 임계값(60점)에 못 미치므로
		 * ambiguous 후보로만 남는다.
		 */
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		MemoryPerson person = createMemoryPerson(
				personId,
				userId,
				"김성은"
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
						"김성이"
				);

		assertEquals(
				PersonMatchType.AMBIGUOUS,
				result.matchType()
		);

		assertEquals(
				List.of(personId),
				result.candidatePersonIds()
		);
	}

	@Test
	void 원문에_가까운_후보가_정규화_수렴_후보보다_ambiguous_순위에서_앞선다() {
		/*
		 * "한영이가"는 안전 조사 "가"를 떼면 "한영이"이고, 이는 어느
		 * 기존 인물의 원문과도 그대로 같지 않다("한영", "한영은"
		 * 모두 다른 문자열). 그래서 EXACT/정규화-EXACT 단계는 모두
		 * 통과하지 못하고 점수 비교로 넘어간다.
		 *
		 * "한영"은 애매한 조사 "이"까지 뗀 형태와 문자열이 같아
		 * ambiguous 점수(30점)를 받고, "한영은"은 편집 거리로만
		 * 약하게(10점) 걸린다. 원문에 더 가까운 근거가 항상 점수
		 * 순위에서 앞서지만, 둘 다 SIMILAR 임계값(60점)에는 못
		 * 미치므로 자동으로 확정되지는 않고 순위가 있는 ambiguous
		 * 후보로 남는다.
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
				PersonMatchType.AMBIGUOUS,
				result.matchType()
		);

		assertEquals(
				List.of(directPersonId, normalizedPersonId),
				result.candidatePersonIds()
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
