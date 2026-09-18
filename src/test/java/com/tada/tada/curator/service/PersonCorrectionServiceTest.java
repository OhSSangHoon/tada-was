package com.tada.tada.curator.service;

import com.tada.tada.curator.dto.PersonCorrectionForm;
import com.tada.tada.curator.entity.MemoryPerson;
import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.entity.MentionCandidateStatus;
import com.tada.tada.curator.entity.MentionEntityType;
import com.tada.tada.curator.repository.MemoryPersonRepository;
import com.tada.tada.curator.repository.MentionCandidateRepository;
import com.tada.tada.diary.entity.Diary;
import com.tada.tada.diary.repository.DiaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonCorrectionServiceTest {

	private MentionCandidateRepository mentionCandidateRepository;
	private MemoryPersonRepository memoryPersonRepository;
	private DiaryRepository diaryRepository;
	private DiaryPersonService diaryPersonService;
	private PersonAggregateService personAggregateService;
	private PersonAliasService personAliasService;

	private PersonCorrectionService personCorrectionService;

	@BeforeEach
	void setUp() {
		mentionCandidateRepository =
				Mockito.mock(MentionCandidateRepository.class);

		memoryPersonRepository =
				Mockito.mock(MemoryPersonRepository.class);

		diaryRepository =
				Mockito.mock(DiaryRepository.class);

		diaryPersonService =
				Mockito.mock(DiaryPersonService.class);

		personAggregateService =
				Mockito.mock(PersonAggregateService.class);

		personAliasService =
				Mockito.mock(
						PersonAliasService.class
				);

		personCorrectionService =
				new PersonCorrectionService(
						mentionCandidateRepository,
						memoryPersonRepository,
						diaryRepository,
						diaryPersonService,
						personAggregateService,
						personAliasService
				);
	}

	@Test
	void Candidate를_다른_사람으로_교정하면_Alias와_DiaryPerson과_Aggregate를_갱신한다() {
		UUID userId = UUID.randomUUID();
		UUID diaryId = UUID.randomUUID();

		MemoryPerson oldPerson =
				MemoryPerson.create(
						userId,
						"민수"
				);

		MemoryPerson targetPerson =
				MemoryPerson.create(
						userId,
						"민혁"
				);

		MentionCandidate candidate =
				MentionCandidate.create(
						diaryId,
						"민혁이랑",
						"민혁",
						MentionEntityType.PERSON,
						MentionCandidateStatus.CONFIRMED,
						oldPerson.getId()
				);

		Diary diary =
				Mockito.mock(Diary.class);

		PersonCorrectionForm form =
				new PersonCorrectionForm();

		form.setTargetPersonId(
				targetPerson.getId()
		);

		when(
				mentionCandidateRepository
						.findDiaryIdById(
								candidate.getId()
						)
		).thenReturn(
				Optional.of(diaryId)
		);

		when(
				diaryRepository
						.findByIdForUpdate(
								diaryId
						)
		).thenReturn(
				Optional.of(diary)
		);

		when(diary.getUserId())
				.thenReturn(userId);

		when(diary.isActive())
				.thenReturn(true);

		when(
				mentionCandidateRepository
						.findByIdForUpdate(
								candidate.getId()
						)
		).thenReturn(
				Optional.of(candidate)
		);

		when(
				memoryPersonRepository
						.findByIdAndUserId(
								oldPerson.getId(),
								userId
						)
		).thenReturn(
				Optional.of(oldPerson)
		);

		when(
				memoryPersonRepository
						.findByIdAndUserId(
								targetPerson.getId(),
								userId
						)
		).thenReturn(
				Optional.of(targetPerson)
		);

		when(
				memoryPersonRepository
						.findByIdForUpdate(
								oldPerson.getId()
						)
		).thenReturn(
				Optional.of(oldPerson)
		);

		when(
				memoryPersonRepository
						.findByIdForUpdate(
								targetPerson.getId()
						)
		).thenReturn(
				Optional.of(targetPerson)
		);

		when(
				mentionCandidateRepository
						.findAllByDiaryId(
								diaryId
						)
		).thenReturn(
				List.of(candidate)
		);

		when(
				diaryPersonService
						.reconcileDiaryPersons(
								diaryId,
								userId,
								List.of(candidate)
						)
		).thenReturn(
				Set.of(targetPerson.getId())
		);

		personCorrectionService.correctPerson(
				userId,
				oldPerson.getId(),
				candidate.getId(),
				form
		);

		assertEquals(
				targetPerson.getId(),
				candidate.getMatchedPersonId()
		);

		verify(memoryPersonRepository)
				.findByIdForUpdate(
						oldPerson.getId()
				);

		verify(memoryPersonRepository)
				.findByIdForUpdate(
						targetPerson.getId()
				);

		verify(
				personAliasService
		).saveIfAbsent(
				userId,
				targetPerson.getId(),
				"민혁이랑"
		);

		verify(diaryPersonService)
				.reconcileDiaryPersons(
						diaryId,
						userId,
						List.of(candidate)
				);

		verify(personAggregateService)
				.recalculate(
						Mockito.eq(userId),
						argThat(
								personIds ->
										personIds.contains(oldPerson.getId())
												&& personIds.contains(targetPerson.getId())
												&& personIds.size() == 2
						)
				);
	}
}