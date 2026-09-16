package com.tada.tada.curator.service;

import com.tada.tada.curator.dto.PersonDetailResponse;
import com.tada.tada.curator.dto.PersonEntityStatResponse;
import com.tada.tada.curator.dto.PersonSummaryResponse;
import com.tada.tada.curator.entity.MemoryPerson;
import com.tada.tada.curator.entity.MentionEntityType;
import com.tada.tada.curator.entity.PersonAggregate;
import com.tada.tada.curator.entity.PersonAlias;
import com.tada.tada.curator.repository.DiaryPersonRepository;
import com.tada.tada.curator.repository.DiaryPersonRepository.PersonStickerRow;
import com.tada.tada.curator.repository.MemoryPersonRepository;
import com.tada.tada.curator.repository.MemoryPersonRepository.PersonListRow;
import com.tada.tada.curator.repository.MentionCandidateRepository;
import com.tada.tada.curator.repository.MentionCandidateRepository.PersonEntityStat;
import com.tada.tada.curator.repository.PersonAggregateRepository;
import com.tada.tada.curator.repository.PersonAliasRepository;
import com.tada.tada.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tada.tada.curator.dto.PersonTimelineItemResponse;
import com.tada.tada.curator.dto.PersonTimelinePageResponse;
import com.tada.tada.curator.dto.PersonTimelineSort;
import com.tada.tada.curator.repository.DiaryPersonRepository.PersonTimelineDiaryRow;
import com.tada.tada.curator.repository.MentionCandidateRepository.PersonTimelineCandidateRow;
import com.tada.tada.curator.repository.MentionCandidateRepository.PersonTimelineKeywordRow;
import com.tada.tada.diary.entity.Sticker;
import com.tada.tada.diary.repository.StickerRepository;
import org.springframework.data.domain.PageRequest;

import java.util.LinkedHashSet;
import java.util.Set;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonQueryService {

	private static final int TOP_ENTITY_LIMIT = 3;
	private static final int TIMELINE_PAGE_SIZE = 20;
	private static final int TIMELINE_FETCH_SIZE = 21;
	private static final int TIMELINE_KEYWORD_LIMIT = 3;

	/* 없는 사람과 남의 사람을 같은 응답으로 처리한다. 403 은 존재 여부를 노출한다. */
	private static final int PERSON_NOT_FOUND_STATUS = 404;

	private static final String PERSON_NOT_FOUND_MESSAGE =
			"사람을 찾을 수 없습니다.";

	private final MemoryPersonRepository memoryPersonRepository;
	private final PersonAggregateRepository personAggregateRepository;
	private final PersonAliasRepository personAliasRepository;
	private final DiaryPersonRepository diaryPersonRepository;
	private final MentionCandidateRepository mentionCandidateRepository;
	private final StickerRepository stickerRepository;

	public List<PersonSummaryResponse> getAllPersons(
			UUID userId
	) {
		requireUserId(userId);

		List<PersonListRow> rows =
				memoryPersonRepository.findPersonList(
						userId
				);

		if (rows.isEmpty()) {
			return List.of();
		}

		List<UUID> personIds =
				new ArrayList<>();

		for (PersonListRow row : rows) {
			personIds.add(
					row.getPersonId()
			);
		}

		Map<UUID, String> stickerUrls =
				loadStickerUrls(
						userId,
						personIds
				);

		Map<UUID, List<String>> aliases =
				loadAliases(
						userId,
						personIds
				);

		List<PersonSummaryResponse> responses =
				new ArrayList<>();

		for (PersonListRow row : rows) {

			UUID personId =
					row.getPersonId();

			responses.add(
					new PersonSummaryResponse(
							personId,
							row.getDisplayName(),
							aliases.getOrDefault(
									personId,
									List.of()
							),
							row.getMentionCount(),
							toLocalDate(
									row.getLastMentionedAt()
							),
							stickerUrls.get(personId)
					)
			);
		}

		return responses;
	}

	/*
	 * 상세 헤더+통계+한눈에 보기.
	 * ACTIVE 기록이 없는 사람은 목록에도 노출되지 않으므로
	 * 상세 조회에서도 존재하지 않는 사람과 동일하게 404 처리한다.
	 */
	public PersonDetailResponse getPersonDetail(
			UUID userId,
			UUID personId
	) {
		requireUserId(userId);

		if (personId == null) {
			throw new IllegalArgumentException(
					"personId must not be null"
			);
		}

		MemoryPerson person =
				memoryPersonRepository
						.findByIdAndUserId(
								personId,
								userId
						)
						.orElseThrow(
								() -> new CustomException(
										PERSON_NOT_FOUND_MESSAGE,
										PERSON_NOT_FOUND_STATUS
								)
						);

		PersonAggregate aggregate =
				personAggregateRepository
						.findById(personId)
						.filter(
								value ->
										value.getMentionCount() > 0
						)
						.orElseThrow(
								() -> new CustomException(
										PERSON_NOT_FOUND_MESSAGE,
										PERSON_NOT_FOUND_STATUS
								)
						);

		int mentionCount =
				aggregate.getMentionCount();

		LocalDate lastMentionedAt =
				toLocalDate(
						aggregate.getLastMentionedAt()
				);

		LocalDate firstMentionedAt =
				diaryPersonRepository.findFirstEntryDate(
						userId,
						personId
				);

		String stickerUrl =
				loadStickerUrls(
						userId,
						List.of(personId)
				)
						.get(personId);

		List<PersonEntityStat> stats =
				mentionCandidateRepository
						.findPersonEntityStats(
								userId,
								personId
						);

		return new PersonDetailResponse(
				person.getId(),
				person.getDisplayName(),
				stickerUrl,
				mentionCount,
				firstMentionedAt,
				lastMentionedAt,
				getTopEntityStats(
						stats,
						MentionEntityType.PLACE
				),
				getTopEntityStats(
						stats,
						MentionEntityType.ACTIVITY
				)
		);
	}

	public PersonTimelinePageResponse getPersonTimeline(
			UUID userId,
			UUID personId,
			PersonTimelineSort sort,
			LocalDate cursor
	) {
		requireUserId(userId);

		if (personId == null) {
			throw new IllegalArgumentException(
					"personId must not be null"
			);
		}

		memoryPersonRepository
				.findByIdAndUserId(
						personId,
						userId
				)
				.orElseThrow(
						() -> new CustomException(
								PERSON_NOT_FOUND_MESSAGE,
								PERSON_NOT_FOUND_STATUS
						)
				);

		PersonTimelineSort effectiveSort =
				sort == null
						? PersonTimelineSort.LATEST
						: sort;

		PageRequest pageRequest =
				PageRequest.of(
						0,
						TIMELINE_FETCH_SIZE
				);

		List<PersonTimelineDiaryRow> fetchedRows;

		if (effectiveSort == PersonTimelineSort.OLDEST) {
			fetchedRows =
					diaryPersonRepository.findTimelineOldest(
							userId,
							personId,
							cursor,
							pageRequest
					);
		} else {
			fetchedRows =
					diaryPersonRepository.findTimelineLatest(
							userId,
							personId,
							cursor,
							pageRequest
					);
		}

		boolean hasNext =
				fetchedRows.size() > TIMELINE_PAGE_SIZE;

		List<PersonTimelineDiaryRow> pageRows;

		if (hasNext) {
			pageRows =
					new ArrayList<>(
							fetchedRows.subList(
									0,
									TIMELINE_PAGE_SIZE
							)
					);
		} else {
			pageRows =
					new ArrayList<>(fetchedRows);
		}

		if (pageRows.isEmpty()) {
			return new PersonTimelinePageResponse(
					List.of(),
					null
			);
		}

		List<UUID> diaryIds =
				new ArrayList<>();

		for (PersonTimelineDiaryRow row : pageRows) {
			diaryIds.add(
					row.getDiaryId()
			);
		}

		Map<UUID, List<UUID>> candidateIdsByDiary =
				loadTimelinePersonCandidateIds(
						userId,
						personId,
						diaryIds
				);

		Map<UUID, String> stickerUrlsByDiary =
				loadTimelineStickerUrls(
						diaryIds
				);

		Map<UUID, List<PersonTimelineKeywordRow>> keywordRowsByDiary =
				loadTimelineKeywordRows(
						userId,
						personId,
						diaryIds
				);

		List<PersonEntityStat> stats =
				mentionCandidateRepository
						.findPersonEntityStats(
								userId,
								personId
						);

		List<PersonTimelineItemResponse> items =
				new ArrayList<>();

		for (PersonTimelineDiaryRow row : pageRows) {

			UUID diaryId =
					row.getDiaryId();

			List<String> keywords =
					selectTimelineKeywords(
							keywordRowsByDiary.getOrDefault(
									diaryId,
									List.of()
							),
							stats
					);

			items.add(
					new PersonTimelineItemResponse(
							diaryId,
							candidateIdsByDiary.getOrDefault(
									diaryId,
									List.of()
							),
							row.getEntryDate(),
							row.getTitle(),
							stickerUrlsByDiary.get(diaryId),
							keywords
					)
			);
		}

		LocalDate nextCursor =
				hasNext
						? pageRows
						.get(pageRows.size() - 1)
						.getEntryDate()
						: null;

		return new PersonTimelinePageResponse(
				items,
				nextCursor
		);
	}


	private List<PersonEntityStatResponse> getTopEntityStats(
			List<PersonEntityStat> stats,
			MentionEntityType entityType
	) {
		List<PersonEntityStatResponse> topStats =
				new ArrayList<>();

		for (PersonEntityStat stat : stats) {

			if (topStats.size() >= TOP_ENTITY_LIMIT) {
				break;
			}

			if (entityType != stat.getEntityType()) {
				continue;
			}

			topStats.add(
					new PersonEntityStatResponse(
							stat.getNormalizedText(),
							stat.getDiaryCount()
					)
			);
		}

		return topStats;
	}

	private Map<UUID, String> loadStickerUrls(
			UUID userId,
			Collection<UUID> personIds
	) {
		List<PersonStickerRow> rows =
				diaryPersonRepository
						.findRepresentativeStickers(
								userId,
								personIds
						);

		Map<UUID, String> stickerUrls =
				new HashMap<>();

		for (PersonStickerRow row : rows) {
			stickerUrls.put(
					row.getPersonId(),
					row.getStickerUrl()
			);
		}

		return stickerUrls;
	}

	/*
	 * alias 순서를 이름순으로 고정한다 — 클라이언트는 순서를 안 쓰지만 응답이 매번 달라지면 테스트가 흔들린다.
	 */
	private Map<UUID, List<String>> loadAliases(
			UUID userId,
			Collection<UUID> personIds
	) {
		List<PersonAlias> aliases =
				personAliasRepository
						.findAllByOwnerUserIdAndPersonIdIn(
								userId,
								personIds
						);

		Map<UUID, List<String>> aliasTexts =
				new HashMap<>();

		for (PersonAlias alias : aliases) {
			aliasTexts
					.computeIfAbsent(
							alias.getPersonId(),
							key -> new ArrayList<>()
					)
					.add(
							alias.getAliasText()
					);
		}

		for (List<String> texts : aliasTexts.values()) {
			texts.sort(
					String::compareTo
			);
		}

		return aliasTexts;
	}

	private static LocalDate toLocalDate(
			LocalDateTime value
	) {
		return value == null
				? null
				: value.toLocalDate();
	}

	private void requireUserId(
			UUID userId
	) {
		if (userId == null) {
			throw new IllegalArgumentException(
					"userId must not be null"
			);
		}
	}

	private Map<UUID, List<UUID>> loadTimelinePersonCandidateIds(
			UUID userId,
			UUID personId,
			List<UUID> diaryIds
	) {
		List<PersonTimelineCandidateRow> rows =
				mentionCandidateRepository
						.findTimelinePersonCandidates(
								userId,
								personId,
								diaryIds
						);

		Map<UUID, List<UUID>> candidateIdsByDiary =
				new HashMap<>();

		for (PersonTimelineCandidateRow row : rows) {
			candidateIdsByDiary
					.computeIfAbsent(
							row.getDiaryId(),
							key -> new ArrayList<>()
					)
					.add(
							row.getPersonCandidateId()
					);
		}

		return candidateIdsByDiary;
	}

	private Map<UUID, String> loadTimelineStickerUrls(
			List<UUID> diaryIds
	) {
		List<Sticker> stickers =
				stickerRepository.findByDiaryIdIn(
						diaryIds
				);

		Map<UUID, String> stickerUrls =
				new HashMap<>();

		for (Sticker sticker : stickers) {
			stickerUrls.put(
					sticker.getDiaryId(),
					sticker.getImageUrl()
			);
		}

		return stickerUrls;
	}

	private Map<UUID, List<PersonTimelineKeywordRow>> loadTimelineKeywordRows(
			UUID userId,
			UUID personId,
			List<UUID> diaryIds
	) {
		List<PersonTimelineKeywordRow> rows =
				mentionCandidateRepository
						.findTimelineKeywords(
								userId,
								personId,
								diaryIds
						);

		Map<UUID, List<PersonTimelineKeywordRow>> rowsByDiary =
				new HashMap<>();

		for (PersonTimelineKeywordRow row : rows) {
			rowsByDiary
					.computeIfAbsent(
							row.getDiaryId(),
							key -> new ArrayList<>()
					)
					.add(row);
		}

		return rowsByDiary;
	}

	private List<String> selectTimelineKeywords(
			List<PersonTimelineKeywordRow> rows,
			List<PersonEntityStat> stats
	) {
		Set<String> places =
				new LinkedHashSet<>();

		Set<String> activities =
				new LinkedHashSet<>();

		for (PersonTimelineKeywordRow row : rows) {

			if (row.getEntityType() == MentionEntityType.PLACE) {
				places.add(
						row.getNormalizedText()
				);
			}

			if (row.getEntityType() == MentionEntityType.ACTIVITY) {
				activities.add(
						row.getNormalizedText()
				);
			}
		}

		List<String> selected =
				new ArrayList<>();

		Set<String> selectedKeys =
				new LinkedHashSet<>();

		addFirstTimelineKeyword(
				selected,
				selectedKeys,
				places,
				MentionEntityType.PLACE,
				stats
		);

		addFirstTimelineKeyword(
				selected,
				selectedKeys,
				activities,
				MentionEntityType.ACTIVITY,
				stats
		);

		for (PersonEntityStat stat : stats) {

			if (selected.size() >= TIMELINE_KEYWORD_LIMIT) {
				break;
			}

			MentionEntityType type =
					stat.getEntityType();

			String text =
					stat.getNormalizedText();

			boolean existsInDiary =
					type == MentionEntityType.PLACE
							? places.contains(text)
							: type == MentionEntityType.ACTIVITY
							&& activities.contains(text);

			if (!existsInDiary) {
				continue;
			}

			String key =
					type.name() + ":" + text;

			if (selectedKeys.add(key)) {
				selected.add(text);
			}
		}

		return selected;
	}

	private void addFirstTimelineKeyword(
			List<String> selected,
			Set<String> selectedKeys,
			Set<String> candidates,
			MentionEntityType entityType,
			List<PersonEntityStat> stats
	) {
		if (candidates.isEmpty()) {
			return;
		}

		for (PersonEntityStat stat : stats) {

			if (stat.getEntityType() != entityType) {
				continue;
			}

			String text =
					stat.getNormalizedText();

			if (!candidates.contains(text)) {
				continue;
			}

			String key =
					entityType.name() + ":" + text;

			if (selectedKeys.add(key)) {
				selected.add(text);
			}

			return;
		}
	}
}