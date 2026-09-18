package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.MemoryPerson;
import com.tada.tada.curator.entity.PersonAggregate;
import com.tada.tada.curator.repository.DiaryPersonRepository;
import com.tada.tada.curator.repository.MemoryPersonRepository;
import com.tada.tada.curator.repository.MentionCandidateRepository;
import com.tada.tada.curator.repository.PersonAggregateRepository;
import com.tada.tada.curator.repository.PersonAliasRepository;
import com.tada.tada.global.exception.CustomException;
import com.tada.tada.diary.repository.StickerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.tada.tada.curator.dto.PersonMemoryGroupResponse;
import com.tada.tada.curator.entity.MentionEntityType;
import com.tada.tada.curator.repository.MentionCandidateRepository.PersonEntityStat;
import com.tada.tada.curator.repository.MentionCandidateRepository.PersonMemoryDiaryRow;
import com.tada.tada.diary.entity.Sticker;
import com.tada.tada.diary.entity.StickerType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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
	private StickerRepository stickerRepository;

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

		stickerRepository =
				Mockito.mock(
						StickerRepository.class
				);

		personQueryService =
				new PersonQueryService(
						memoryPersonRepository,
						personAggregateRepository,
						personAliasRepository,
						diaryPersonRepository,
						mentionCandidateRepository,
						stickerRepository
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

	@Test
	void 사람_추억은_3건_이상_그룹만_정렬하고_대표_스티커를_최대_3개_선택한다() {
		UUID userId = UUID.randomUUID();

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
						5,
						LocalDate.of(2026, 9, 10)
				);

		PersonEntityStat excluded =
				createStat(
						MentionEntityType.ACTIVITY,
						"산책",
						2,
						LocalDate.of(2026, 9, 1),
						LocalDate.of(2026, 9, 20)
				);

		PersonEntityStat cafe =
				createStat(
						MentionEntityType.PLACE,
						"카페",
						5,
						LocalDate.of(2026, 9, 6),
						LocalDate.of(2026, 9, 10)
				);

		PersonEntityStat basketball =
				createStat(
						MentionEntityType.ACTIVITY,
						"농구",
						5,
						LocalDate.of(2026, 8, 1),
						LocalDate.of(2026, 9, 1)
				);

		PersonEntityStat park =
				createStat(
						MentionEntityType.PLACE,
						"공원",
						3,
						LocalDate.of(2026, 7, 1),
						LocalDate.of(2026, 9, 1)
				);

		PersonEntityStat school =
				createStat(
						MentionEntityType.PLACE,
						"학교",
						3,
						LocalDate.of(2026, 6, 1),
						LocalDate.of(2026, 9, 1)
				);

		UUID d1 = UUID.randomUUID();
		UUID d2 = UUID.randomUUID();
		UUID d3 = UUID.randomUUID();
		UUID d4 = UUID.randomUUID();
		UUID d5 = UUID.randomUUID();

		List<PersonMemoryDiaryRow> memoryRows =
				List.of(
						createMemoryRow(
								MentionEntityType.PLACE,
								"카페",
								d1,
								LocalDate.of(2026, 9, 6),
								"카페 1"
						),
						createMemoryRow(
								MentionEntityType.PLACE,
								"카페",
								d2,
								LocalDate.of(2026, 9, 7),
								"카페 2"
						),
						createMemoryRow(
								MentionEntityType.PLACE,
								"카페",
								d3,
								LocalDate.of(2026, 9, 8),
								"카페 3"
						),
						createMemoryRow(
								MentionEntityType.PLACE,
								"카페",
								d4,
								LocalDate.of(2026, 9, 9),
								"카페 4"
						),
						createMemoryRow(
								MentionEntityType.PLACE,
								"카페",
								d5,
								LocalDate.of(2026, 9, 10),
								"카페 5"
						)
				);

		Sticker oldest =
				createSticker(
						d1,
						"url-1",
						"날씨"
				);

		Sticker third =
				createSticker(
						d2,
						"url-2",
						"대화"
				);

		Sticker second =
				createSticker(
						d3,
						"url-3",
						"커피"
				);

		Sticker duplicateKeyword =
				createSticker(
						d4,
						"url-4",
						"친구"
				);

		Sticker newest =
				createSticker(
						d5,
						"url-5",
						"친구"
				);

		when(
				memoryPersonRepository.findByIdAndUserId(
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

		when(
				mentionCandidateRepository.findPersonEntityStats(
						userId,
						personId
				)
		).thenReturn(
				List.of(
						excluded,
						school,
						park,
						basketball,
						cafe
				)
		);

		when(
				mentionCandidateRepository.findPersonMemoryDiaries(
						userId,
						personId
				)
		).thenReturn(
				memoryRows
		);

		when(
				stickerRepository.findByDiaryIdIn(
						any()
				)
		).thenReturn(
				List.of(
						oldest,
						third,
						second,
						duplicateKeyword,
						newest
				)
		);

		List<PersonMemoryGroupResponse> result =
				personQueryService.getPersonMemories(
						userId,
						personId
				);

		assertEquals(
				4,
				result.size()
		);

		assertEquals(
				"카페",
				result.get(0).getGroupKey()
		);

		assertEquals(
				"농구",
				result.get(1).getGroupKey()
		);

		assertEquals(
				"공원",
				result.get(2).getGroupKey()
		);

		assertEquals(
				"학교",
				result.get(3).getGroupKey()
		);

		assertEquals(
				List.of("url-5", "url-3", "url-2"),
				result.get(0)
						.getStickers()
						.stream()
						.map(sticker -> sticker.getImageUrl())
						.toList()
		);

		assertEquals(
				List.of("친구", "커피", "대화"),
				result.get(0)
						.getStickers()
						.stream()
						.map(sticker -> sticker.getKeyword())
						.toList()
		);
	}

	private PersonEntityStat createStat(
			MentionEntityType entityType,
			String normalizedText,
			long diaryCount,
			LocalDate firstEntryDate,
			LocalDate lastEntryDate
	) {
		PersonEntityStat stat =
				Mockito.mock(
						PersonEntityStat.class
				);

		when(stat.getEntityType())
				.thenReturn(entityType);

		when(stat.getNormalizedText())
				.thenReturn(normalizedText);

		when(stat.getDiaryCount())
				.thenReturn(diaryCount);

		when(stat.getFirstEntryDate())
				.thenReturn(firstEntryDate);

		when(stat.getLastEntryDate())
				.thenReturn(lastEntryDate);

		return stat;
	}

	private PersonMemoryDiaryRow createMemoryRow(
			MentionEntityType entityType,
			String normalizedText,
			UUID diaryId,
			LocalDate entryDate,
			String title
	) {
		PersonMemoryDiaryRow row =
				Mockito.mock(
						PersonMemoryDiaryRow.class
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

		return row;
	}

	private Sticker createSticker(
			UUID diaryId,
			String imageUrl,
			String keyword
	) {
		return Sticker.builder()
				.diaryId(diaryId)
				.imageUrl(imageUrl)
				.keyword(keyword)
				.type(StickerType.EXTRACTED)
				.build();
	}
}