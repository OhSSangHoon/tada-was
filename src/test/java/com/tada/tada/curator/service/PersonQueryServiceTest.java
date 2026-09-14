package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.MemoryPerson;
import com.tada.tada.curator.entity.PersonAggregate;
import com.tada.tada.curator.repository.DiaryPersonRepository;
import com.tada.tada.curator.repository.MemoryPersonRepository;
import com.tada.tada.curator.repository.MentionCandidateRepository;
import com.tada.tada.curator.repository.PersonAggregateRepository;
import com.tada.tada.curator.repository.PersonAliasRepository;
import com.tada.tada.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonQueryServiceTest {
	
	private MemoryPersonRepository memoryPersonRepository;
	private PersonAggregateRepository personAggregateRepository;
	private PersonAliasRepository personAliasRepository;
	private DiaryPersonRepository diaryPersonRepository;
	private MentionCandidateRepository mentionCandidateRepository;
	
	private PersonQueryService personQueryService;
	
	@BeforeEach
	void setUp() {
		memoryPersonRepository =
				Mockito.mock(
						MemoryPersonRepository.class
				);
		
		personAggregateRepository =
				Mockito.mock(
						PersonAggregateRepository.class
				);
		
		personAliasRepository =
				Mockito.mock(
						PersonAliasRepository.class
				);
		
		diaryPersonRepository =
				Mockito.mock(
						DiaryPersonRepository.class
				);
		
		mentionCandidateRepository =
				Mockito.mock(
						MentionCandidateRepository.class
				);
		
		personQueryService =
				new PersonQueryService(
						memoryPersonRepository,
						personAggregateRepository,
						personAliasRepository,
						diaryPersonRepository,
						mentionCandidateRepository
				);
	}
	
	@Test
	void ACTIVE_기록이_없는_사람은_상세_조회에서_404를_반환한다() {
		UUID userId =
				UUID.randomUUID();
		
		MemoryPerson person =
				MemoryPerson.create(
						userId,
						"민수"
				);
		
		UUID personId =
				person.getId();
		
		PersonAggregate aggregate =
				PersonAggregate.create(
						personId,
						0,
						null
				);
		
		when(
				memoryPersonRepository
						.findByIdAndUserId(
								personId,
								userId
						)
		).thenReturn(
				Optional.of(person)
		);
		
		when(
				personAggregateRepository.findById(
						personId
				)
		).thenReturn(
				Optional.of(aggregate)
		);
		
		CustomException exception =
				assertThrows(
						CustomException.class,
						() ->
								personQueryService
										.getPersonDetail(
												userId,
												personId
										)
				);
		
		assertEquals(
				404,
				exception.getStatusCode()
		);
		
		assertEquals(
				"사람을 찾을 수 없습니다.",
				exception.getMessage()
		);
		
		verify(
				diaryPersonRepository,
				never()
		).findFirstEntryDate(
				userId,
				personId
		);
	}
	
	@Test
	void PersonAggregate가_없는_사람도_상세_조회에서_404를_반환한다() {
		UUID userId =
				UUID.randomUUID();
		
		MemoryPerson person =
				MemoryPerson.create(
						userId,
						"민수"
				);
		
		UUID personId =
				person.getId();
		
		when(
				memoryPersonRepository
						.findByIdAndUserId(
								personId,
								userId
						)
		).thenReturn(
				Optional.of(person)
		);
		
		when(
				personAggregateRepository.findById(
						personId
				)
		).thenReturn(
				Optional.empty()
		);
		
		CustomException exception =
				assertThrows(
						CustomException.class,
						() ->
								personQueryService
										.getPersonDetail(
												userId,
												personId
										)
				);
		
		assertEquals(
				404,
				exception.getStatusCode()
		);
		
		verify(
				diaryPersonRepository,
				never()
		).findFirstEntryDate(
				userId,
				personId
		);
	}
}