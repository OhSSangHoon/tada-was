package com.tada.tada.curator.service;

import com.tada.tada.curator.dto.MemoryRecallResponse;
import com.tada.tada.curator.dto.MemoryRecallType;
import com.tada.tada.curator.entity.MentionEntityType;
import com.tada.tada.curator.repository.DiaryPersonRepository;
import com.tada.tada.curator.repository.MemoryPersonRepository;
import com.tada.tada.curator.repository.MentionCandidateRepository;
import com.tada.tada.curator.repository.MentionCandidateRepository.MemoryRecallEntityRow;
import com.tada.tada.diary.entity.DiaryStatus;
import com.tada.tada.diary.repository.DiaryRepository;
import com.tada.tada.diary.repository.StickerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class MemoryRecallServiceTest {

	private DiaryRepository diaryRepository;
	private DiaryPersonRepository diaryPersonRepository;
	private MemoryPersonRepository memoryPersonRepository;
	private MentionCandidateRepository mentionCandidateRepository;
	private StickerRepository stickerRepository;

	private MemoryRecallService memoryRecallService;

	@BeforeEach
	void setUp() {
		diaryRepository =
				Mockito.mock(DiaryRepository.class);

		diaryPersonRepository =
				Mockito.mock(DiaryPersonRepository.class);

		memoryPersonRepository =
				Mockito.mock(MemoryPersonRepository.class);

		mentionCandidateRepository =
				Mockito.mock(MentionCandidateRepository.class);

		stickerRepository =
				Mockito.mock(StickerRepository.class);

		memoryRecallService =
				new MemoryRecallService(
						diaryRepository,
						diaryPersonRepository,
						memoryPersonRepository,
						mentionCandidateRepository,
						stickerRepository
				);
	}

	@Test
	void ACTIVITY가_한_개의_일기에만_등장하면_다시_꺼내본_일기_후보에서_제외한다() {
		UUID userId = UUID.randomUUID();
		UUID diaryId = UUID.randomUUID();

		MemoryRecallEntityRow row =
				createEntityRow(
						MentionEntityType.ACTIVITY,
						"농구",
						diaryId,
						LocalDate.of(2026, 8, 10),
						"농구한 날",
						"친구들과 농구를 했다."
				);

		when(
				diaryPersonRepository
						.findMemoryRecallPersonRows(userId)
		).thenReturn(
				List.of()
		);

		when(
				mentionCandidateRepository
						.findMemoryRecallEntityRows(userId)
		).thenReturn(
				List.of(row)
		);

		stubNoOtherRecallCandidates(userId);

		MemoryRecallResponse response =
				memoryRecallService.getMemoryRecall(
						userId
				);

		assertNull(response);
	}

	@Test
	void ACTIVITY가_서로_다른_두_일기에_등장하면_ACTIVITY_타입으로_후보_중_하나를_선택한다() {
		UUID userId = UUID.randomUUID();

		UUID firstDiaryId =
				UUID.randomUUID();

		UUID secondDiaryId =
				UUID.randomUUID();

		MemoryRecallEntityRow firstRow =
				createEntityRow(
						MentionEntityType.ACTIVITY,
						"농구",
						firstDiaryId,
						LocalDate.of(2026, 8, 10),
						"첫 번째 농구",
						"친구들과 농구를 했다."
				);

		MemoryRecallEntityRow secondRow =
				createEntityRow(
						MentionEntityType.ACTIVITY,
						"농구",
						secondDiaryId,
						LocalDate.of(2026, 8, 20),
						"두 번째 농구",
						"오늘도 농구를 했다."
				);

		when(
				diaryPersonRepository
						.findMemoryRecallPersonRows(userId)
		).thenReturn(
				List.of()
		);

		when(
				mentionCandidateRepository
						.findMemoryRecallEntityRows(userId)
		).thenReturn(
				List.of(
						firstRow,
						secondRow
				)
		);

		stubNoOtherRecallCandidates(userId);

		when(
				stickerRepository.findByDiaryIdIn(
						any()
				)
		).thenReturn(
				List.of()
		);

		when(
				mentionCandidateRepository.findAllByDiaryId(
						any()
				)
		).thenReturn(
				List.of()
		);

		MemoryRecallResponse response =
				memoryRecallService.getMemoryRecall(
						userId
				);

		assertEquals(
				MemoryRecallType.ACTIVITY,
				response.getEventType()
		);

		assertTrue(
				Set.of(
						firstDiaryId,
						secondDiaryId
				).contains(
						response.getDiaryId()
				)
		);
	}

	private MemoryRecallEntityRow createEntityRow(
			MentionEntityType entityType,
			String normalizedText,
			UUID diaryId,
			LocalDate entryDate,
			String title,
			String content
	) {
		MemoryRecallEntityRow row =
				Mockito.mock(
						MemoryRecallEntityRow.class
				);

		when(row.getEntityType())
				.thenReturn(entityType);

		when(row.getNormalizedText())
				.thenReturn(normalizedText);

		when(row.getDiaryId())
				.thenReturn(diaryId);

		when(row.getEntryDate())
				.thenReturn(entryDate);

		when(row.getTitle())
				.thenReturn(title);

		when(row.getContent())
				.thenReturn(content);

		return row;
	}

	private void stubNoOtherRecallCandidates(
			UUID userId
	) {
		when(
				diaryRepository
						.findByUserIdAndEntryDateBetweenAndStatus(
								eq(userId),
								any(LocalDate.class),
								any(LocalDate.class),
								eq(DiaryStatus.ACTIVE)
						)
		).thenReturn(
				List.of()
		);

		when(
				diaryRepository.findByUserIdAndStatus(
						userId,
						DiaryStatus.ACTIVE
				)
		).thenReturn(
				List.of()
		);
	}
}