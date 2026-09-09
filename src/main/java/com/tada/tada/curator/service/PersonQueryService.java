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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/*
 * 사람 목록과 사람 상세의 조회 전용 서비스.
 * 조회는 항상 personId 와 현재 사용자 id 를 함께 쓴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonQueryService {

	/* 자주 나온 장소 / 자주 한 일은 각각 최대 3개 */
	private static final int TOP_ENTITY_LIMIT = 3;

	/* 없는 사람과 남의 사람을 같은 응답으로 처리한다. 403 은 존재 여부를 노출한다. */
	private static final int PERSON_NOT_FOUND_STATUS = 404;

	private static final String PERSON_NOT_FOUND_MESSAGE =
			"사람을 찾을 수 없습니다.";

	private final MemoryPersonRepository memoryPersonRepository;
	private final PersonAggregateRepository personAggregateRepository;
	private final PersonAliasRepository personAliasRepository;
	private final DiaryPersonRepository diaryPersonRepository;
	private final MentionCandidateRepository mentionCandidateRepository;

	/*
	 * 홈 사람 목록.
	 *
	 * 목록 / Sticker / alias 를 각각 한 번씩만 읽는다.
	 * 사람 수와 무관하게 쿼리는 항상 3번이다.
	 * 빈 목록은 404 가 아니라 200 + [] 다.
	 */
	public List<PersonSummaryResponse> getPersonList(
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
	 * 사람 상세의 헤더 + 통계 + 한눈에 보기.
	 * 기록 수와 최근 기록일은 PersonAggregate 캐시를 읽고, 처음 기록일만 계산한다.
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

		Optional<PersonAggregate> aggregate =
				personAggregateRepository.findById(
						personId
				);

		int mentionCount =
				aggregate
						.map(PersonAggregate::getMentionCount)
						.orElse(0);

		LocalDate lastMentionedAt =
				aggregate
						.map(PersonAggregate::getLastMentionedAt)
						.map(PersonQueryService::toLocalDate)
						.orElse(null);

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
				topOf(
						stats,
						MentionEntityType.PLACE
				),
				topOf(
						stats,
						MentionEntityType.ACTIVITY
				)
		);
	}

	/*
	 * 이미 정렬된 집계에서 한 종류만 골라 앞에서 자른다.
	 * 정렬은 쿼리에서 끝나 있으므로 여기서 다시 정렬하지 않는다.
	 */
	private List<PersonEntityStatResponse> topOf(
			List<PersonEntityStat> stats,
			MentionEntityType entityType
	) {
		List<PersonEntityStatResponse> top =
				new ArrayList<>();

		for (PersonEntityStat stat : stats) {

			if (top.size() >= TOP_ENTITY_LIMIT) {
				break;
			}

			if (entityType != stat.getEntityType()) {
				continue;
			}

			top.add(
					new PersonEntityStatResponse(
							stat.getNormalizedText(),
							stat.getDiaryCount()
					)
			);
		}

		return top;
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
	 * alias 순서를 이름순으로 고정한다.
	 * 클라이언트 필터는 순서를 쓰지 않지만 응답이 호출마다 달라지면 테스트가 흔들린다.
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
}
