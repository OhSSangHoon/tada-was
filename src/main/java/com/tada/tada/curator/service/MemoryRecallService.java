package com.tada.tada.curator.service;

import com.tada.tada.curator.dto.MemoryRecallResponse;
import com.tada.tada.curator.dto.MemoryRecallType;
import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.entity.MentionCandidateStatus;
import com.tada.tada.curator.entity.MentionEntityType;
import com.tada.tada.curator.repository.DiaryPersonRepository;
import com.tada.tada.curator.repository.DiaryPersonRepository.MemoryRecallPersonRow;
import com.tada.tada.curator.repository.MemoryPersonRepository;
import com.tada.tada.curator.repository.MentionCandidateRepository;
import com.tada.tada.curator.repository.MentionCandidateRepository.MemoryRecallEntityRow;
import com.tada.tada.diary.entity.Diary;
import com.tada.tada.diary.entity.DiaryStatus;
import com.tada.tada.diary.repository.DiaryRepository;
import com.tada.tada.diary.repository.StickerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.BreakIterator;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoryRecallService {

	private static final ZoneId KST =
			ZoneId.of("Asia/Seoul");

	private static final int DATE_RANGE_DAYS = 3;

	private final DiaryRepository diaryRepository;
	private final DiaryPersonRepository diaryPersonRepository;
	private final MemoryPersonRepository memoryPersonRepository;
	private final MentionCandidateRepository mentionCandidateRepository;
	private final StickerRepository stickerRepository;

	private boolean isExcludedDiary(
			UUID diaryId,
			UUID excludeDiaryId
	) {
		return excludeDiaryId != null
				&& excludeDiaryId.equals(diaryId);
	}

	private List<Diary> findMonthRecallCandidates(
			UUID userId,
			int monthsAgo,
			UUID excludeDiaryId
	) {
		LocalDate targetDate =
				LocalDate.now(KST)
						.minusMonths(monthsAgo);

		List<Diary> diaries =
				diaryRepository
						.findByUserIdAndEntryDateBetweenAndStatus(
								userId,
								targetDate.minusDays(
										DATE_RANGE_DAYS
								),
								targetDate.plusDays(
										DATE_RANGE_DAYS
								),
								DiaryStatus.ACTIVE
						);

		if (excludeDiaryId == null) {
			return diaries;
		}

		return diaries.stream()
				.filter(
						diary ->
								!isExcludedDiary(
										diary.getId(),
										excludeDiaryId
								)
				)
				.toList();
	}

	private List<Diary> findSameWeekdayCandidates(
			UUID userId,
			UUID excludeDiaryId
	) {
		LocalDate today =
				LocalDate.now(KST);

		DayOfWeek todayWeekday =
				today.getDayOfWeek();

		List<Diary> result =
				new ArrayList<>();

		for (Diary diary :
				diaryRepository.findByUserIdAndStatus(
						userId,
						DiaryStatus.ACTIVE
				)) {

			LocalDate entryDate =
					diary.getEntryDate();

			/*
			 * 메시지가 "지난 X요일"이므로
			 * 오늘 및 미래 날짜의 일기는 제외한다.
			 */
			if (!entryDate.isBefore(today)) {
				continue;
			}

			if (isExcludedDiary(
					diary.getId(),
					excludeDiaryId
			)) {
				continue;
			}

			if (entryDate.getDayOfWeek()
					== todayWeekday) {

				result.add(diary);
			}
		}

		return result;
	}

	private Diary findFirstEntry(
			UUID userId
	) {
		return diaryRepository
				.findByUserIdAndStatus(
						userId,
						DiaryStatus.ACTIVE
				)
				.stream()
				.min(
						Comparator
								.comparing(
										Diary::getEntryDate
								)
								.thenComparing(
										Diary::getId
								)
				)
				.orElse(null);
	}

	private List<Diary> findFallbackCandidates(
			UUID userId,
			UUID excludeDiaryId
	) {
		List<Diary> all =
				diaryRepository
						.findByUserIdAndStatus(
								userId,
								DiaryStatus.ACTIVE
						);

		if (excludeDiaryId == null) {
			return all;
		}

		List<Diary> filtered =
				all.stream()
						.filter(
								diary ->
										!isExcludedDiary(
												diary.getId(),
												excludeDiaryId
										)
						)
						.toList();

		/*
		 * 수동 새로고침 시 현재 Diary를 우선 제외한다.
		 * 다만 ACTIVE Diary가 현재 카드 하나뿐이라면
		 * 같은 카드가 다시 선택되는 것을 허용한다.
		 */
		return filtered.isEmpty()
				? all
				: filtered;
	}

	private record RecallDiary(
			UUID diaryId,
			LocalDate entryDate,
			String title,
			String content
	) {
	}

	private record RecallGroup(
			MemoryRecallType eventType,
			String groupKey,
			String displayValue,
			List<RecallDiary> diaries
	) {
	}

	private record RecallSelection(
			RecallDiary diary,
			String displayValue
	) {
	}

	private List<RecallGroup> findPersonRecallGroups(
			UUID userId,
			UUID excludeDiaryId
	) {
		List<MemoryRecallPersonRow> rows =
				diaryPersonRepository
						.findMemoryRecallPersonRows(
								userId
						);

		Map<UUID, List<RecallDiary>> diariesByPerson =
				new LinkedHashMap<>();

		Map<UUID, String> displayNameByPerson =
				new LinkedHashMap<>();

		for (MemoryRecallPersonRow row : rows) {

			if (isExcludedDiary(
					row.getDiaryId(),
					excludeDiaryId
			)) {
				continue;
			}

			displayNameByPerson.putIfAbsent(
					row.getPersonId(),
					row.getDisplayName()
			);

			diariesByPerson
					.computeIfAbsent(
							row.getPersonId(),
							ignored ->
									new ArrayList<>()
					)
					.add(
							new RecallDiary(
									row.getDiaryId(),
									row.getEntryDate(),
									row.getTitle(),
									row.getContent()
							)
					);
		}

		List<RecallGroup> groups =
				new ArrayList<>();

		for (Map.Entry<UUID, List<RecallDiary>> entry :
				diariesByPerson.entrySet()) {

			UUID personId =
					entry.getKey();

			if (entry.getValue().isEmpty()) {
				continue;
			}

			groups.add(
					new RecallGroup(
							MemoryRecallType.PERSON,
							personId.toString(),
							displayNameByPerson.get(
									personId
							),
							entry.getValue()
					)
			);
		}

		return groups;
	}

	private List<RecallGroup> findEntityRecallGroups(
			UUID userId,
			UUID excludeDiaryId
	) {
		List<MemoryRecallEntityRow> rows =
				mentionCandidateRepository
						.findMemoryRecallEntityRows(
								userId
						);

		Map<String, Map<UUID, RecallDiary>> diariesByGroup =
				new LinkedHashMap<>();

		Map<String, MentionEntityType> typeByGroup =
				new LinkedHashMap<>();

		Map<String, String> valueByGroup =
				new LinkedHashMap<>();

		for (MemoryRecallEntityRow row : rows) {

			String groupKey =
					row.getEntityType().name()
							+ ":"
							+ row.getNormalizedText();

			typeByGroup.putIfAbsent(
					groupKey,
					row.getEntityType()
			);

			valueByGroup.putIfAbsent(
					groupKey,
					row.getNormalizedText()
			);

			diariesByGroup
					.computeIfAbsent(
							groupKey,
							ignored ->
									new LinkedHashMap<>()
					)
					.putIfAbsent(
							row.getDiaryId(),
							new RecallDiary(
									row.getDiaryId(),
									row.getEntryDate(),
									row.getTitle(),
									row.getContent()
							)
					);
		}

		List<RecallGroup> groups =
				new ArrayList<>();

		for (Map.Entry<String, Map<UUID, RecallDiary>> entry :
				diariesByGroup.entrySet()) {

			MentionEntityType entityType =
					typeByGroup.get(
							entry.getKey()
					);

			List<RecallDiary> originalDiaries =
					new ArrayList<>(
							entry.getValue().values()
					);

			/*
			 * ACTIVITY의 "반복" 여부는 excludeDiaryId를
			 * 적용하기 전 전체 ACTIVE Diary 기준으로 판단한다.
			 */
			if (entityType
					== MentionEntityType.ACTIVITY
					&& originalDiaries.size() < 2) {

				continue;
			}

			List<RecallDiary> selectableDiaries =
					originalDiaries.stream()
							.filter(
									diary ->
											!isExcludedDiary(
													diary.diaryId(),
													excludeDiaryId
											)
							)
							.toList();

			if (selectableDiaries.isEmpty()) {
				continue;
			}

			MemoryRecallType eventType =
					entityType
							== MentionEntityType.PLACE
							? MemoryRecallType.PLACE
							: MemoryRecallType.ACTIVITY;

			groups.add(
					new RecallGroup(
							eventType,
							entry.getKey(),
							valueByGroup.get(
									entry.getKey()
							),
							selectableDiaries
					)
			);
		}

		return groups;
	}

	private MemoryRecallType selectEventType(
			UUID userId,
			List<RecallGroup> personGroups,
			List<RecallGroup> entityGroups,
			UUID excludeDiaryId
	) {
		List<MemoryRecallType> availableTypes =
				new ArrayList<>();

		if (!findMonthRecallCandidates(
				userId,
				12,
				excludeDiaryId
		).isEmpty()) {

			availableTypes.add(
					MemoryRecallType.TWELVE_MONTHS_AGO
			);
		}

		if (!findMonthRecallCandidates(
				userId,
				6,
				excludeDiaryId
		).isEmpty()) {

			availableTypes.add(
					MemoryRecallType.SIX_MONTHS_AGO
			);
		}

		if (!findMonthRecallCandidates(
				userId,
				3,
				excludeDiaryId
		).isEmpty()) {

			availableTypes.add(
					MemoryRecallType.THREE_MONTHS_AGO
			);
		}

		if (!personGroups.isEmpty()) {
			availableTypes.add(
					MemoryRecallType.PERSON
			);
		}

		boolean hasPlace =
				entityGroups.stream()
						.anyMatch(
								group ->
										group.eventType()
												== MemoryRecallType.PLACE
						);

		if (hasPlace) {
			availableTypes.add(
					MemoryRecallType.PLACE
			);
		}

		boolean hasActivity =
				entityGroups.stream()
						.anyMatch(
								group ->
										group.eventType()
												== MemoryRecallType.ACTIVITY
						);

		if (hasActivity) {
			availableTypes.add(
					MemoryRecallType.ACTIVITY
			);
		}

		if (!findSameWeekdayCandidates(
				userId,
				excludeDiaryId
		).isEmpty()) {

			availableTypes.add(
					MemoryRecallType.SAME_WEEKDAY
			);
		}

		Diary firstEntry =
				findFirstEntry(userId);

		if (firstEntry != null
				&& !isExcludedDiary(
				firstEntry.getId(),
				excludeDiaryId
		)) {

			availableTypes.add(
					MemoryRecallType.FIRST_ENTRY
			);
		}

		if (availableTypes.isEmpty()) {
			return MemoryRecallType.FALLBACK;
		}

		return availableTypes.get(
				ThreadLocalRandom.current()
						.nextInt(
								availableTypes.size()
						)
		);
	}

	private RecallSelection selectRecallSelection(
			UUID userId,
			MemoryRecallType eventType,
			List<RecallGroup> personGroups,
			List<RecallGroup> entityGroups,
			UUID excludeDiaryId
	) {
		return switch (eventType) {

			case TWELVE_MONTHS_AGO ->
					new RecallSelection(
							selectRandomDiary(
									findMonthRecallCandidates(
											userId,
											12,
											excludeDiaryId
									)
							),
							null
					);

			case SIX_MONTHS_AGO ->
					new RecallSelection(
							selectRandomDiary(
									findMonthRecallCandidates(
											userId,
											6,
											excludeDiaryId
									)
							),
							null
					);

			case THREE_MONTHS_AGO ->
					new RecallSelection(
							selectRandomDiary(
									findMonthRecallCandidates(
											userId,
											3,
											excludeDiaryId
									)
							),
							null
					);

			case PERSON -> {
				RecallGroup group =
						selectRandomGroup(
								personGroups
						);

				yield new RecallSelection(
						selectRandomRecallDiary(
								group.diaries()
						),
						group.displayValue()
				);
			}

			case PLACE -> {
				List<RecallGroup> groups =
						entityGroups.stream()
								.filter(
										group ->
												group.eventType()
														== MemoryRecallType.PLACE
								)
								.toList();

				RecallGroup group =
						selectRandomGroup(
								groups
						);

				yield new RecallSelection(
						selectRandomRecallDiary(
								group.diaries()
						),
						group.displayValue()
				);
			}

			case ACTIVITY -> {
				List<RecallGroup> groups =
						entityGroups.stream()
								.filter(
										group ->
												group.eventType()
														== MemoryRecallType.ACTIVITY
								)
								.toList();

				RecallGroup group =
						selectRandomGroup(
								groups
						);

				yield new RecallSelection(
						selectRandomRecallDiary(
								group.diaries()
						),
						group.displayValue()
				);
			}

			case SAME_WEEKDAY ->
					new RecallSelection(
							selectRandomDiary(
									findSameWeekdayCandidates(
											userId,
											excludeDiaryId
									)
							),
							null
					);

			case FIRST_ENTRY -> {
				Diary diary =
						findFirstEntry(
								userId
						);

				yield new RecallSelection(
						diary == null
								? null
								: toRecallDiary(
								diary
						),
						null
				);
			}

			case FALLBACK ->
					new RecallSelection(
							selectRandomDiary(
									findFallbackCandidates(
											userId,
											excludeDiaryId
									)
							),
							null
					);
		};
	}

	private RecallGroup selectRandomGroup(
			List<RecallGroup> groups
	) {
		return groups.get(
				ThreadLocalRandom.current()
						.nextInt(
								groups.size()
						)
		);
	}

	private RecallDiary selectRandomRecallDiary(
			List<RecallDiary> diaries
	) {
		return diaries.get(
				ThreadLocalRandom.current()
						.nextInt(
								diaries.size()
						)
		);
	}

	private RecallDiary selectRandomDiary(
			List<Diary> diaries
	) {
		if (diaries.isEmpty()) {
			return null;
		}

		Diary diary =
				diaries.get(
						ThreadLocalRandom.current()
								.nextInt(
										diaries.size()
								)
				);

		return toRecallDiary(
				diary
		);
	}

	private RecallDiary toRecallDiary(
			Diary diary
	) {
		return new RecallDiary(
				diary.getId(),
				diary.getEntryDate(),
				diary.getTitle(),
				diary.getContent()
		);
	}

	private String findStickerUrl(
			UUID diaryId
	) {
		return stickerRepository
				.findByDiaryIdIn(
						List.of(
								diaryId
						)
				)
				.stream()
				.findFirst()
				.map(
						sticker ->
								sticker.getImageUrl()
				)
				.orElse(null);
	}

	private List<String> findRecallTags(
			UUID userId,
			UUID diaryId
	) {
		List<MentionCandidate> candidates =
				mentionCandidateRepository
						.findAllByDiaryId(
								diaryId
						);

		Set<String> tagSet =
				new LinkedHashSet<>();

		for (MentionCandidate candidate :
				candidates) {

			if (candidate.getStatus()
					!= MentionCandidateStatus.CONFIRMED) {

				continue;
			}

			if (candidate.getEntityType()
					== MentionEntityType.PERSON) {

				UUID personId =
						candidate.getMatchedPersonId();

				if (personId == null) {
					continue;
				}

				memoryPersonRepository
						.findByIdAndUserId(
								personId,
								userId
						)
						.ifPresent(
								person ->
										tagSet.add(
												person.getDisplayName()
										)
						);

				continue;
			}

			if (candidate.getEntityType()
					== MentionEntityType.PLACE
					|| candidate.getEntityType()
					== MentionEntityType.ACTIVITY) {

				tagSet.add(
						candidate.getNormalizedText()
				);
			}
		}

		List<String> tags =
				new ArrayList<>(
						tagSet
				);

		Collections.shuffle(
				tags
		);

		if (tags.size() <= 2) {
			return tags;
		}

		int tagCount =
				ThreadLocalRandom.current()
						.nextBoolean()
						? 2
						: 3;

		return new ArrayList<>(
				tags.subList(
						0,
						Math.min(
								tagCount,
								tags.size()
						)
				)
		);
	}

	private String buildContentPreview(
			String content
	) {
		if (content == null
				|| content.isBlank()) {

			return null;
		}

		String text =
				content.strip();

		BreakIterator iterator =
				BreakIterator.getSentenceInstance(
						Locale.KOREAN
				);

		iterator.setText(
				text
		);

		int start =
				iterator.first();

		int end =
				iterator.next();

		if (end
				== BreakIterator.DONE) {

			return text;
		}

		return text.substring(
				start,
				end
		).strip();
	}

	private String findFirstPersonName(
			UUID userId,
			UUID diaryId
	) {
		List<MentionCandidate> candidates =
				mentionCandidateRepository
						.findAllByDiaryId(
								diaryId
						);

		candidates.sort(
				Comparator.comparing(
						MentionCandidate::getId
				)
		);

		for (MentionCandidate candidate :
				candidates) {

			if (candidate.getEntityType()
					!= MentionEntityType.PERSON) {

				continue;
			}

			if (candidate.getStatus()
					!= MentionCandidateStatus.CONFIRMED) {

				continue;
			}

			UUID personId =
					candidate.getMatchedPersonId();

			if (personId == null) {
				continue;
			}

			var person =
					memoryPersonRepository
							.findByIdAndUserId(
									personId,
									userId
							);

			if (person.isPresent()) {
				return person.get()
						.getDisplayName();
			}
		}

		return null;
	}

	private boolean hasFinalConsonant(
			String value
	) {
		if (value == null
				|| value.isBlank()) {

			return false;
		}

		char last =
				value.charAt(
						value.length() - 1
				);

		if (last < 0xAC00
				|| last > 0xD7A3) {

			return false;
		}

		return (last - 0xAC00)
				% 28 != 0;
	}

	private String appendAndParticle(
			String value
	) {
		return value
				+ (
				hasFinalConsonant(value)
						? "과"
						: "와"
		);
	}

	private String appendSubjectParticle(
			String value
	) {
		return value
				+ (
				hasFinalConsonant(value)
						? "이가"
						: "가"
		);
	}

	private String getKoreanWeekday(
			DayOfWeek dayOfWeek
	) {
		return switch (dayOfWeek) {
			case MONDAY -> "월요일";
			case TUESDAY -> "화요일";
			case WEDNESDAY -> "수요일";
			case THURSDAY -> "목요일";
			case FRIDAY -> "금요일";
			case SATURDAY -> "토요일";
			case SUNDAY -> "일요일";
		};
	}

	private String buildMessage(
			UUID userId,
			MemoryRecallType eventType,
			RecallSelection selection
	) {
		if (selection == null
				|| selection.diary() == null) {

			return null;
		}

		RecallDiary diary =
				selection.diary();

		return switch (eventType) {

			case TWELVE_MONTHS_AGO -> {
				String personName =
						findFirstPersonName(
								userId,
								diary.diaryId()
						);

				if (personName == null) {
					yield "작년 이맘때, 이런 하루를 보냈어요.";
				}

				yield "작년 이맘때, "
						+ appendAndParticle(
						personName
				)
						+ " 이런 하루를 보냈어요.";
			}

			case SIX_MONTHS_AGO ->
					"6개월 전, 이런 하루를 보냈어요.";

			case THREE_MONTHS_AGO ->
					"3개월 전, 이런 하루를 보냈어요.";

			case PERSON ->
					"갑자기 "
							+ appendSubjectParticle(
							selection.displayValue()
					)
							+ " 떠오르는 날이네요.";

			case PLACE ->
					selection.displayValue()
							+ ", 오랜만이죠?";

			case ACTIVITY ->
					"갑자기 "
							+ selection.displayValue()
							+ "했던 날이 떠오르네요.";

			case SAME_WEEKDAY ->
					"지난 "
							+ getKoreanWeekday(
							LocalDate.now(KST)
									.getDayOfWeek()
					)
							+ "엔 이런 하루를 보냈어요.";

			case FIRST_ENTRY ->
					"처음 기록을 시작한 날이에요.";

			case FALLBACK ->
					null;
		};
	}

	public MemoryRecallResponse getMemoryRecall(
			UUID userId
	) {
		return getMemoryRecall(
				userId,
				null
		);
	}

	public MemoryRecallResponse getMemoryRecall(
			UUID userId,
			UUID excludeDiaryId
	) {
		if (userId == null) {
			throw new IllegalArgumentException(
					"userId must not be null"
			);
		}

		List<RecallGroup> personGroups =
				findPersonRecallGroups(
						userId,
						excludeDiaryId
				);

		List<RecallGroup> entityGroups =
				findEntityRecallGroups(
						userId,
						excludeDiaryId
				);

		MemoryRecallType eventType =
				selectEventType(
						userId,
						personGroups,
						entityGroups,
						excludeDiaryId
				);

		RecallSelection selection =
				selectRecallSelection(
						userId,
						eventType,
						personGroups,
						entityGroups,
						excludeDiaryId
				);

		if (selection == null
				|| selection.diary() == null) {

			return null;
		}

		RecallDiary diary =
				selection.diary();

		return new MemoryRecallResponse(
				eventType,
				buildMessage(
						userId,
						eventType,
						selection
				),
				diary.diaryId(),
				diary.entryDate(),
				diary.title(),
				buildContentPreview(
						diary.content()
				),
				findStickerUrl(
						diary.diaryId()
				),
				findRecallTags(
						userId,
						diary.diaryId()
				)
		);
	}
}