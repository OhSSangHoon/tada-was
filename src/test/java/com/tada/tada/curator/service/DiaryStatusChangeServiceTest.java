package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.DiaryPerson;
import com.tada.tada.curator.repository.DiaryPersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DiaryStatusChangeServiceTest {

	private DiaryPersonRepository diaryPersonRepository;
	private PersonAggregateService personAggregateService;
	private DiaryStatusChangeService diaryStatusChangeService;

	@BeforeEach
	void setUp() {
		diaryPersonRepository =
				Mockito.mock(DiaryPersonRepository.class);

		personAggregateService =
				Mockito.mock(PersonAggregateService.class);

		diaryStatusChangeService =
				new DiaryStatusChangeService(
						diaryPersonRepository,
						personAggregateService
				);
	}

	@Test
	void 연결된_인물_전체를_재계산_대상으로_넘긴다() {
		UUID diaryId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID personA = UUID.randomUUID();
		UUID personB = UUID.randomUUID();

		when(diaryPersonRepository.findAllByDiaryId(diaryId))
				.thenReturn(
						List.of(
								DiaryPerson.create(diaryId, personA),
								DiaryPerson.create(diaryId, personB)
						)
				);

		diaryStatusChangeService
				.recalculateAffectedPersons(diaryId, userId);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Set<UUID>> captor =
				ArgumentCaptor.forClass(Set.class);

		verify(personAggregateService)
				.recalculate(eq(userId), captor.capture());

		assertEquals(
				Set.of(personA, personB),
				captor.getValue()
		);
	}

	/*
	 * Trash 는 물리삭제가 아니라 상태 전이다.
	 * Candidate / Relation / DiaryPerson 을 건드리면 Restore 가 깨진다.
	 */
	@Test
	void 연결_데이터를_삭제하지_않는다() {
		UUID diaryId = UUID.randomUUID();

		when(diaryPersonRepository.findAllByDiaryId(diaryId))
				.thenReturn(
						List.of(
								DiaryPerson.create(
										diaryId,
										UUID.randomUUID()
								)
						)
				);

		diaryStatusChangeService.recalculateAffectedPersons(
				diaryId,
				UUID.randomUUID()
		);

		verify(diaryPersonRepository, never())
				.deleteByDiaryId(any());

		verify(diaryPersonRepository, never())
				.deleteAll();
	}

	@Test
	void 인물이_없으면_재계산을_호출하지_않는다() {
		UUID diaryId = UUID.randomUUID();

		when(diaryPersonRepository.findAllByDiaryId(diaryId))
				.thenReturn(List.of());

		diaryStatusChangeService.recalculateAffectedPersons(
				diaryId,
				UUID.randomUUID()
		);

		verify(personAggregateService, never())
				.recalculate(any(), any());
	}

	@Test
	void 같은_인물이_여러_Candidate로_연결돼도_한_번만_넘긴다() {
		UUID diaryId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID person = UUID.randomUUID();

		when(diaryPersonRepository.findAllByDiaryId(diaryId))
				.thenReturn(
						List.of(
								DiaryPerson.create(diaryId, person),
								DiaryPerson.create(diaryId, person)
						)
				);

		diaryStatusChangeService
				.recalculateAffectedPersons(diaryId, userId);

		verify(personAggregateService)
				.recalculate(eq(userId), eq(Set.of(person)));
	}

	@Test
	void null_인자를_거부한다() {
		assertThrows(
				IllegalArgumentException.class,
				() -> diaryStatusChangeService
						.recalculateAffectedPersons(
								null,
								UUID.randomUUID()
						)
		);

		assertThrows(
				IllegalArgumentException.class,
				() -> diaryStatusChangeService
						.recalculateAffectedPersons(
								UUID.randomUUID(),
								null
						)
		);

		verify(personAggregateService, never())
				.recalculate(any(), any());
	}
}
