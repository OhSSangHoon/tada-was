package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.entity.MentionCandidateStatus;
import com.tada.tada.curator.entity.MentionEntityType;
import com.tada.tada.curator.service.DiaryPersonService;
import com.tada.tada.curator.service.MentionCandidatePersonRefService;
import com.tada.tada.curator.service.MentionCandidateService;
import com.tada.tada.curator.service.PersonAggregateService;
import com.tada.tada.curator.service.PersonCreationGuard;
import com.tada.tada.curator.service.PersonMatchingService;
import com.tada.tada.curator.service.PersonNormalizer;
import com.tada.tada.curator.service.PersonResolverService;
import com.tada.tada.curator.repository.MemoryPersonRepository;
import com.tada.tada.curator.repository.MentionCandidateRepository;
import com.tada.tada.curator.repository.PersonAliasRepository;
import com.tada.tada.curator.entity.MemoryPerson;
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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

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
	void 이미_처리된_일기의_중복_이벤트는_다시_처리하지_않는다() {
		UUID diaryId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		Diary diary =
				Diary.builder()
						.userId(userId)
						.entryDate(LocalDate.now())
						.title("오늘")
						.weather(null)
						.content("민수를 만났다")
						.build();

		ExtractionResult extractionResult =
				new ExtractionResult(
						List.of(),
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
				diaryRepository.findByIdForUpdate(diaryId)
		).thenReturn(
				Optional.of(diary)
		);

		when(
				mentionCandidateService.hasCandidates(
						diaryId
				)
		).thenReturn(true);

		processor.process(event);

		verify(
				mentionCandidateService
		).hasCandidates(diaryId);

		verify(
				extractionResultValidator,
				never()
		).validate(
				Mockito.any(),
				Mockito.any()
		);

		verifyNoInteractions(
				relationService,
				diaryPersonService,
				personAggregateService
		);
	}

	@Test
	void 휴지통_일기의_추출_이벤트는_처리하지_않는다() {
		UUID diaryId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		Diary diary = Diary.builder()
				.userId(userId)
				.entryDate(LocalDate.now())
				.title("오늘")
				.content("민수를 만났다")
				.build();
		diary.trash();

		MentionExtractedEvent event = new MentionExtractedEvent(
				diaryId,
				userId,
				new ExtractionResult(List.of(), List.of(), List.of())
		);

		when(diaryRepository.findByIdForUpdate(diaryId))
				.thenReturn(Optional.of(diary));

		assertThrows(IllegalStateException.class, () -> processor.process(event));
		verifyNoInteractions(
				extractionResultValidator,
				mentionCandidateService,
				relationService,
				diaryPersonService,
				personAggregateService
		);
	}

	@Test
	void 같은_rawText의_서로_다른_ref는_같은_인물을_재사용할_수_있다() {
		UUID diaryId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();
		Diary diary = Diary.builder()
				.userId(userId)
				.entryDate(LocalDate.now())
				.title("오늘")
				.content("민수와 민수를 만났다")
				.build();

		MentionCandidate first = MentionCandidate.create(
				diaryId,
				"민수",
				"민수",
				MentionEntityType.PERSON,
				MentionCandidateStatus.CONFIRMED,
				personId
		);
		MentionCandidate second = MentionCandidate.create(
				diaryId,
				"민수",
				"민수",
				MentionEntityType.PERSON,
				MentionCandidateStatus.CONFIRMED,
				personId
		);

		ExtractionResult extractionResult = new ExtractionResult(
				List.of(
						new PersonExtraction("p1", "민수", "PERSON"),
						new PersonExtraction("p2", "민수", "PERSON")
				),
				List.of(),
				List.of()
		);

		when(diaryRepository.findByIdForUpdate(diaryId))
				.thenReturn(Optional.of(diary));
		/*
		 * 두 번째 ref 는 assignedPersonIds 가 비어 있지 않으므로
		 * 마지막 인자는 matcher 로 받는다.
		 */
		when(mentionCandidateService.createPersonCandidate(
				diaryId,
				userId,
				"민수",
				Set.of()
		)).thenReturn(first, second);
		when(diaryPersonService.reconcileDiaryPersons(
				Mockito.eq(diaryId),
				Mockito.eq(userId),
				Mockito.anyList()
		)).thenReturn(Set.of(personId));

		processor.process(new MentionExtractedEvent(
				diaryId,
				userId,
				extractionResult
		));

		verify(mentionCandidateService, times(2))
				.createPersonCandidate(
						diaryId,
						userId,
						"민수",
						Set.of()
				);
	}

	@Test
	void 같은_rawText의_재사용은_실제_matching과_resolver_경로에서도_동작한다() {
		UUID diaryId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();
		Diary diary = Diary.builder()
				.userId(userId)
				.entryDate(LocalDate.now())
				.title("오늘")
				.content("민수와 민수를 만났다")
				.build();

		MemoryPerson person = Mockito.mock(MemoryPerson.class);
		MemoryPersonRepository memoryPersonRepository =
				Mockito.mock(MemoryPersonRepository.class);
		PersonAliasRepository personAliasRepository =
				Mockito.mock(PersonAliasRepository.class);
		MentionCandidateRepository candidateRepository =
				Mockito.mock(MentionCandidateRepository.class);

		PersonNormalizer normalizer = new PersonNormalizer();
		PersonMatchingService matchingService = new PersonMatchingService(
				normalizer,
				memoryPersonRepository,
				personAliasRepository
		);
		PersonResolverService resolverService = new PersonResolverService(
				matchingService,
				new PersonCreationGuard(candidateRepository),
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

		when(person.getId()).thenReturn(personId);
		when(person.getUserId()).thenReturn(userId);
		when(person.getDisplayName()).thenReturn("민수");
		when(memoryPersonRepository.findAllByUserIdAndDisplayNameIn(
				eq(userId),
				any()
		)).thenReturn(List.of(person));
		when(personAliasRepository.findAllByOwnerUserIdAndNormalizedTextIn(
				eq(userId),
				any()
		)).thenReturn(List.of());
		when(memoryPersonRepository.findById(personId))
				.thenReturn(Optional.of(person));
		when(candidateRepository.save(any(MentionCandidate.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(diaryRepository.findByIdForUpdate(diaryId))
				.thenReturn(Optional.of(diary));
		when(diaryPersonService.reconcileDiaryPersons(
				eq(diaryId),
				eq(userId),
				any()
		)).thenReturn(Set.of(personId));

		ExtractionResult extractionResult = new ExtractionResult(
				List.of(
						new PersonExtraction("p1", "민수", "PERSON"),
						new PersonExtraction("p2", "민수", "PERSON")
				),
				List.of(),
				List.of()
		);

		realProcessor.process(new MentionExtractedEvent(
				diaryId,
				userId,
				extractionResult
		));

		verify(candidateRepository, times(2))
				.save(any(MentionCandidate.class));
		verify(memoryPersonRepository, never())
				.save(any(MemoryPerson.class));
	}
}
