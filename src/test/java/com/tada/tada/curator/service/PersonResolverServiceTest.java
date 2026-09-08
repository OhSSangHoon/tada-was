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
import static org.mockito.Mockito.doAnswer;
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
		MemoryPerson person = Mockito.mock(MemoryPerson.class);

		when(personMatchingService.match(
				userId,
				"민수",
				Set.of()
		)).thenReturn(
				PersonMatchResult.exact(personId)
		);
		when(memoryPersonRepository.findById(personId))
				.thenReturn(Optional.of(person));
		when(person.getUserId()).thenReturn(userId);
		when(person.getId()).thenReturn(personId);

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
		MemoryPerson person = Mockito.mock(MemoryPerson.class);

		when(personMatchingService.match(
				userId,
				"김민혁이",
				Set.of()
		)).thenReturn(
				PersonMatchResult.similar(personId)
		);
		when(memoryPersonRepository.findById(personId))
				.thenReturn(Optional.of(person));
		when(person.getUserId()).thenReturn(userId);
		when(person.getId()).thenReturn(personId);

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

		MemoryPerson person = Mockito.mock(MemoryPerson.class);

		when(personMatchingService.match(
				userId,
				"민혁상",
				Set.of()
		)).thenReturn(
				PersonMatchResult.ambiguous(
						java.util.List.of(
								personId
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
		when(person.getUserId()).thenReturn(userId);

		UUID result =
				personResolverService.resolve(
						userId,
						"민혁상"
				);

		assertEquals(
				personId,
				result
		);

		verify(
				memoryPersonRepository,
				never()
		).save(any());
	}

	@Test
	void ambiguous의_후보가_아닌_creationGuard_결과는_재사용하지_않는다() {
		UUID userId = UUID.randomUUID();
		UUID candidateId = UUID.randomUUID();
		UUID unrelatedId = UUID.randomUUID();

		when(personMatchingService.match(userId, "민서", Set.of()))
				.thenReturn(PersonMatchResult.ambiguous(
						java.util.List.of(candidateId)
				));
		when(personCreationGuard.findReusablePerson(
				userId, "민서", "민서", Set.of()
		)).thenReturn(Optional.of(unrelatedId));
		when(memoryPersonRepository.save(any()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		UUID result = personResolverService.resolve(userId, "민서");

		assertNotNull(result);
		verify(memoryPersonRepository, never()).findById(unrelatedId);
		verify(memoryPersonRepository).save(any(MemoryPerson.class));
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
	void 신규_인물의_표시_이름은_매칭용_저장값보다_원문을_지킨다() {
		/*
		 * "민수씨"의 저장용 normalizedText 는 "민수"지만
		 * 표시 이름까지 줄이면 실제 이름이 훼손될 수 있다.
		 */
		assertNewPersonDisplayName(
				"민수씨",
				"민수",
				"민수씨"
		);
	}

	@Test
	void 신규_인물의_displayName은_이름_끝일_수_있는_접미사를_보존한다() {
		/*
		 * 표시 이름을 줄여 버리면 "김성은"이 기존 "김성"과
		 * 잘못 병합되어 다른 사람의 기억이 합쳐진다.
		 */
		assertNewPersonDisplayName("김성은", "김성", "김성은");
		assertNewPersonDisplayName("가을이", "가을", "가을이");
		assertNewPersonDisplayName("민혁이", "민혁", "민혁이");
		assertNewPersonDisplayName("김사랑", "김사", "김사랑");
		assertNewPersonDisplayName("이영도", "이영", "이영도");
		assertNewPersonDisplayName("민수도", "민수", "민수도");
		assertNewPersonDisplayName("김선아", "김선", "김선아");
	}

	@Test
	void 신규_인물의_displayName에서_확실한_조사는_제거한다() {
		assertNewPersonDisplayName(
				"민수와",
				"민수",
				"민수"
		);

		assertNewPersonDisplayName(
				"한영이가",
				"한영",
				"한영이"
		);

		assertNewPersonDisplayName(
				"민수에게도",
				"민수",
				"민수"
		);
	}

	private void assertNewPersonDisplayName(
			String rawText,
			String expectedNormalizedText,
			String expectedDisplayName
	) {
		UUID userId = UUID.randomUUID();

		when(personMatchingService.match(
				userId,
				rawText,
				Set.of()
		)).thenReturn(
				PersonMatchResult.newPerson()
		);

		when(personCreationGuard.findReusablePerson(
				userId,
				rawText,
				expectedNormalizedText,
				Set.of()
		)).thenReturn(
				Optional.empty()
		);

		/*
		 * when(...).thenAnswer(...) 를 쓰면
		 * 두 번째 호출부터 when(save(any())) 자체가
		 * 이전에 등록한 Answer 를 null 인자로 실행해 NPE 가 난다.
		 * 여러 번 재스텁하는 헬퍼이므로 doAnswer 를 사용한다.
		 */
		doAnswer(invocation -> {
			MemoryPerson person =
					invocation.getArgument(0);

			assertEquals(
					expectedDisplayName,
					person.getDisplayName()
			);

			assertEquals(
					userId,
					person.getUserId()
			);

			return person;
		}).when(memoryPersonRepository).save(any());

		assertNotNull(
				personResolverService.resolve(
						userId,
						rawText
				)
		);
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
	void exact_결과가_다른_사용자의_인물이면_거부한다() {
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();
		MemoryPerson otherPerson = MemoryPerson.create(
				UUID.randomUUID(),
				"민수"
		);

		when(personMatchingService.match(userId, "민수", Set.of()))
				.thenReturn(PersonMatchResult.exact(personId));
		when(memoryPersonRepository.findById(personId))
				.thenReturn(Optional.of(otherPerson));

		assertThrows(
				IllegalStateException.class,
				() -> personResolverService.resolve(userId, "민수")
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
