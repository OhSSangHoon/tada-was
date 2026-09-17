package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.MemoryPerson;
import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.entity.MentionCandidateStatus;
import com.tada.tada.curator.entity.MentionEntityType;
import com.tada.tada.curator.repository.MemoryPersonRepository;
import com.tada.tada.curator.repository.MentionCandidateRepository;
import com.tada.tada.curator.repository.PersonAliasRepository;
import com.tada.tada.curator.validation.ExtractionResultValidator;
import com.tada.tada.diary.entity.Diary;
import com.tada.tada.diary.repository.DiaryRepository;
import com.tada.tada.global.event.MentionExtractedEvent;
import com.tada.tada.global.event.dto.ExtractionResult;
import com.tada.tada.global.event.dto.PersonExtraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MentionExtractionProcessorTest {

	private DiaryRepository diaryRepository;
	private ExtractionResultValidator extractionResultValidator;
	private MentionCandidateService mentionCandidateService;
	private MentionCandidatePersonRefService relationService;
	private DiaryPersonService diaryPersonService;
	private PersonAggregateService personAggregateService;

	private MentionExtractionProcessor processor;

	@BeforeEach
	void setUp() {
		diaryRepository =
				Mockito.mock(DiaryRepository.class);

		extractionResultValidator =
				Mockito.mock(ExtractionResultValidator.class);

		mentionCandidateService =
				Mockito.mock(MentionCandidateService.class);

		relationService =
				Mockito.mock(
						MentionCandidatePersonRefService.class
				);

		diaryPersonService =
				Mockito.mock(DiaryPersonService.class);

		personAggregateService =
				Mockito.mock(PersonAggregateService.class);

		processor =
				new MentionExtractionProcessor(
						diaryRepository,
						extractionResultValidator,
						mentionCandidateService,
						relationService,
						diaryPersonService,
						personAggregateService,
						new PersonNormalizer()
				);
	}

	@Test
	void 휴지통_일기의_추출_이벤트는_처리하지_않는다() {
		UUID diaryId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		Diary diary =
				Diary.builder()
						.userId(userId)
						.entryDate(LocalDate.now())
						.title("오늘")
						.content("민수를 만났다")
						.build();

		diary.trash();

		MentionExtractedEvent event =
				new MentionExtractedEvent(
						diaryId,
						userId,
						new ExtractionResult(
								List.of(),
								List.of(),
								List.of()
						)
				);

		when(
				diaryRepository.findByIdForUpdate(
						diaryId
				)
		).thenReturn(
				Optional.of(diary)
		);

		assertThrows(
				IllegalStateException.class,
				() -> processor.process(event)
		);

		verifyNoInteractions(
				extractionResultValidator,
				mentionCandidateService,
				relationService,
				diaryPersonService,
				personAggregateService
		);
	}

	@Test
	void 같은_normalizedText의_서로_다른_ref는_확정된_인물을_직접_재사용한다() {
		UUID diaryId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		Diary diary =
				Diary.builder()
						.userId(userId)
						.entryDate(LocalDate.now())
						.title("오늘")
						.content("민수와 민수를 만났다")
						.build();

		MentionCandidate first =
				MentionCandidate.create(
						diaryId,
						"민수",
						"민수",
						MentionEntityType.PERSON,
						MentionCandidateStatus.CONFIRMED,
						personId
				);

		MentionCandidate second =
				MentionCandidate.create(
						diaryId,
						"민수",
						"민수",
						MentionEntityType.PERSON,
						MentionCandidateStatus.CONFIRMED,
						personId
				);

		ExtractionResult extractionResult =
				new ExtractionResult(
						List.of(
								new PersonExtraction(
										"p1",
										"민수",
										"PERSON"
								),
								new PersonExtraction(
										"p2",
										"민수",
										"PERSON"
								)
						),
						List.of(),
						List.of()
				);

		when(
				diaryRepository.findByIdForUpdate(
						diaryId
				)
		).thenReturn(
				Optional.of(diary)
		);

		when(
				mentionCandidateService.findAllByDiaryId(
						diaryId
				)
		).thenReturn(
				List.of()
		);

		when(
				mentionCandidateService.createPersonCandidate(
						diaryId,
						userId,
						"민수",
						Set.of()
				)
		).thenReturn(
				first
		);

		when(
				mentionCandidateService
						.createPersonCandidateForMatchedPerson(
								diaryId,
								"민수",
								personId
						)
		).thenReturn(
				second
		);

		when(
				diaryPersonService.reconcileDiaryPersons(
						eq(diaryId),
						eq(userId),
						Mockito.anyList()
				)
		).thenReturn(
				Set.of(personId)
		);

		processor.process(
				new MentionExtractedEvent(
						diaryId,
						userId,
						extractionResult
				)
		);

		verify(
				mentionCandidateService
		).createPersonCandidate(
				diaryId,
				userId,
				"민수",
				Set.of()
		);

		verify(
				mentionCandidateService
		).createPersonCandidateForMatchedPerson(
				diaryId,
				"민수",
				personId
		);

		verify(
				mentionCandidateService,
				times(1)
		).createPersonCandidate(
				eq(diaryId),
				eq(userId),
				eq("민수"),
				Mockito.anySet()
		);

		verify(
				diaryPersonService
		).reconcileDiaryPersons(
				eq(diaryId),
				eq(userId),
				Mockito.argThat(
						candidates ->
								candidates.size() == 2
										&& candidates.contains(first)
										&& candidates.contains(second)
				)
		);

		verify(
				personAggregateService
		).recalculate(
				userId,
				Set.of(personId)
		);
	}

	@Test
	void 같은_rawText의_재사용은_실제_matching과_resolver_경로에서도_동작한다() {
		UUID diaryId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		Diary diary =
				Diary.builder()
						.userId(userId)
						.entryDate(LocalDate.now())
						.title("오늘")
						.content("민수와 민수를 만났다")
						.build();

		MemoryPerson person =
				Mockito.mock(MemoryPerson.class);

		MemoryPersonRepository memoryPersonRepository =
				Mockito.mock(
						MemoryPersonRepository.class
				);

		PersonAliasRepository personAliasRepository =
				Mockito.mock(
						PersonAliasRepository.class
				);

		MentionCandidateRepository candidateRepository =
				Mockito.mock(
						MentionCandidateRepository.class
				);

		PersonNormalizer normalizer =
				new PersonNormalizer();

		PersonMatchingService matchingService =
				new PersonMatchingService(
						normalizer,
						memoryPersonRepository,
						personAliasRepository
				);

		PersonResolverService resolverService =
				new PersonResolverService(
						matchingService,
						new PersonCreationGuard(
								candidateRepository,
								normalizer
						),
						normalizer,
						memoryPersonRepository
				);

		MentionCandidateService realCandidateService =
				new MentionCandidateService(
						candidateRepository,
						resolverService,
						normalizer
				);

		MentionExtractionProcessor realProcessor =
				new MentionExtractionProcessor(
						diaryRepository,
						extractionResultValidator,
						realCandidateService,
						relationService,
						diaryPersonService,
						personAggregateService,
						normalizer
				);

		when(
				person.getId()
		).thenReturn(
				personId
		);

		when(
				person.getUserId()
		).thenReturn(
				userId
		);

		when(
				person.getDisplayName()
		).thenReturn(
				"민수"
		);

		when(
				memoryPersonRepository
						.findAllByUserIdAndDisplayNameIn(
								eq(userId),
								any()
						)
		).thenReturn(
				List.of(person)
		);

		when(
				personAliasRepository
						.findAllByOwnerUserIdAndNormalizedTextIn(
								eq(userId),
								any()
						)
		).thenReturn(
				List.of()
		);

		when(
				personAliasRepository
						.findAllByOwnerUserIdAndAliasTextIn(
								eq(userId),
								any()
						)
		).thenReturn(
				List.of()
		);

		when(
				memoryPersonRepository.findById(
						personId
				)
		).thenReturn(
				Optional.of(person)
		);

		when(
				candidateRepository.findAllByDiaryId(
						diaryId
				)
		).thenReturn(
				List.of()
		);

		when(
				candidateRepository.save(
						any(MentionCandidate.class)
				)
		).thenAnswer(
				invocation ->
						invocation.getArgument(0)
		);

		when(
				diaryRepository.findByIdForUpdate(
						diaryId
				)
		).thenReturn(
				Optional.of(diary)
		);

		when(
				diaryPersonService.reconcileDiaryPersons(
						eq(diaryId),
						eq(userId),
						any()
				)
		).thenReturn(
				Set.of(personId)
		);

		ExtractionResult extractionResult =
				new ExtractionResult(
						List.of(
								new PersonExtraction(
										"p1",
										"민수",
										"PERSON"
								),
								new PersonExtraction(
										"p2",
										"민수",
										"PERSON"
								)
						),
						List.of(),
						List.of()
				);

		realProcessor.process(
				new MentionExtractedEvent(
						diaryId,
						userId,
						extractionResult
				)
		);

		verify(
				candidateRepository,
				times(2)
		).save(
				any(MentionCandidate.class)
		);

		verify(
				memoryPersonRepository,
				never()
		).save(
				any(MemoryPerson.class)
		);
	}

	@Test
	void 같은_일기의_성_포함_생략_이름은_자동으로_같은_인물을_재사용하지_않는다() {
		UUID diaryId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		UUID fullNamePersonId =
				UUID.randomUUID();

		UUID shortNamePersonId =
				UUID.randomUUID();

		Diary diary =
				Diary.builder()
						.userId(userId)
						.entryDate(LocalDate.now())
						.title("오늘")
						.content("김민혁과 민혁을 만났다")
						.build();

		MentionCandidate fullNameCandidate =
				MentionCandidate.create(
						diaryId,
						"김민혁",
						"김민혁",
						MentionEntityType.PERSON,
						MentionCandidateStatus.CONFIRMED,
						fullNamePersonId
				);

		MentionCandidate shortNameCandidate =
				MentionCandidate.create(
						diaryId,
						"민혁",
						"민혁",
						MentionEntityType.PERSON,
						MentionCandidateStatus.CONFIRMED,
						shortNamePersonId
				);

		ExtractionResult extractionResult =
				new ExtractionResult(
						List.of(
								new PersonExtraction(
										"p1",
										"김민혁",
										"PERSON"
								),
								new PersonExtraction(
										"p2",
										"민혁",
										"PERSON"
								)
						),
						List.of(),
						List.of()
				);

		when(
				diaryRepository.findByIdForUpdate(
						diaryId
				)
		).thenReturn(
				Optional.of(diary)
		);

		when(
				mentionCandidateService.findAllByDiaryId(
						diaryId
				)
		).thenReturn(
				List.of()
		);

		when(
				mentionCandidateService.createPersonCandidate(
						diaryId,
						userId,
						"김민혁",
						Set.of()
				)
		).thenReturn(
				fullNameCandidate
		);

		when(
				mentionCandidateService.createPersonCandidate(
						diaryId,
						userId,
						"민혁",
						Set.of(fullNamePersonId)
				)
		).thenReturn(
				shortNameCandidate
		);

		when(
				diaryPersonService.reconcileDiaryPersons(
						eq(diaryId),
						eq(userId),
						any()
				)
		).thenReturn(
				Set.of(
						fullNamePersonId,
						shortNamePersonId
				)
		);

		processor.process(
				new MentionExtractedEvent(
						diaryId,
						userId,
						extractionResult
				)
		);

		verify(
				mentionCandidateService
		).createPersonCandidate(
				diaryId,
				userId,
				"김민혁",
				Set.of()
		);

		verify(
				mentionCandidateService
		).createPersonCandidate(
				diaryId,
				userId,
				"민혁",
				Set.of(fullNamePersonId)
		);

		verify(
				mentionCandidateService,
				never()
		).createPersonCandidateForMatchedPerson(
				eq(diaryId),
				eq("민혁"),
				any()
		);
	}

	@Test
	void 동일_이벤트_재처리는_기존_Candidate를_KEEP하고_새로_생성하지_않는다() {
		UUID diaryId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		Diary diary =
				Diary.builder()
						.userId(userId)
						.entryDate(LocalDate.now())
						.title("오늘")
						.weather(null)
						.content("민수를 만났다")
						.build();

		MentionCandidate existingCandidate =
				MentionCandidate.create(
						diaryId,
						"민수",
						"민수",
						MentionEntityType.PERSON,
						MentionCandidateStatus.CONFIRMED,
						personId
				);

		UUID existingCandidateId =
				existingCandidate.getId();

		ExtractionResult extractionResult =
				new ExtractionResult(
						List.of(
								new PersonExtraction(
										"p1",
										"민수",
										"PERSON"
								)
						),
						List.of(),
						List.of()
				);

		MentionExtractedEvent event =
				new MentionExtractedEvent(
						diaryId,
						userId,
						extractionResult
				);

		when(
				diaryRepository.findByIdForUpdate(
						diaryId
				)
		).thenReturn(
				Optional.of(diary)
		);

		when(
				mentionCandidateService.findAllByDiaryId(
						diaryId
				)
		).thenReturn(
				List.of(existingCandidate)
		);

		when(
				diaryPersonService.reconcileDiaryPersons(
						eq(diaryId),
						eq(userId),
						Mockito.anyList()
				)
		).thenReturn(
				Set.of(personId)
		);

		processor.process(event);

		verify(
				extractionResultValidator
		).validate(
				"민수를 만났다",
				extractionResult
		);

		verify(
				mentionCandidateService,
				never()
		).createPersonCandidate(
				eq(diaryId),
				eq(userId),
				Mockito.anyString(),
				Mockito.anySet()
		);

		verify(
				mentionCandidateService,
				never()
		).createPersonCandidateForMatchedPerson(
				any(),
				Mockito.anyString(),
				any()
		);

		verify(
				diaryPersonService
		).reconcileDiaryPersons(
				eq(diaryId),
				eq(userId),
				Mockito.argThat(
						candidates ->
								candidates.size() == 1
										&& candidates.iterator().next()
										== existingCandidate
				)
		);

		verify(
				personAggregateService
		).recalculate(
				userId,
				Set.of(personId)
		);

		assertEquals(
				existingCandidateId,
				existingCandidate.getId()
		);

		assertEquals(
				MentionCandidateStatus.CONFIRMED,
				existingCandidate.getStatus()
		);

		assertEquals(
				personId,
				existingCandidate.getMatchedPersonId()
		);
	}
}