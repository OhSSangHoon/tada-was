package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.entity.MentionCandidateStatus;
import com.tada.tada.curator.policy.PersonMatchingPolicy;
import com.tada.tada.curator.repository.MentionCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonCreationGuard {

	private final MentionCandidateRepository mentionCandidateRepository;
	private final PersonNormalizer personNormalizer;

	public Optional<UUID> findReusablePerson(
			UUID userId,
			String rawText,
			String normalizedText
	) {
		return findReusablePerson(
				userId,
				rawText,
				normalizedText,
				Set.of()
		);
	}
	
	public Optional<UUID> findReusablePerson(
			UUID userId,
			String rawText,
			String normalizedText,
			Set<UUID> blockedPersonIds
	) {
		return findReusablePerson(
				userId,
				rawText,
				normalizedText,
				blockedPersonIds,
				Set.of()
		);
	}
	
	public Optional<UUID> findReusablePerson(
			UUID userId,
			String rawText,
			String normalizedText,
			Set<UUID> blockedPersonIds,
			Set<UUID> currentCandidatePersonIds
	) {
		if (userId == null) {
			throw new IllegalArgumentException(
					"userId must not be null"
			);
		}

		String safeRawText =
				rawText == null
						? ""
						: rawText;

		String safeNormalizedText =
				normalizedText == null
						? ""
						: normalizedText;

		if (safeRawText.isBlank()
				&& safeNormalizedText.isBlank()) {
			return Optional.empty();
		}
		
		Set<UUID> blockedIds =
				blockedPersonIds == null
						? Set.of()
						: Set.copyOf(blockedPersonIds);
		
		Set<UUID> currentCandidateIds =
				currentCandidatePersonIds == null
						? Set.of()
						: Set.copyOf(currentCandidatePersonIds);

		List<MentionCandidate> histories =
				mentionCandidateRepository
						.findPersonMatchHistory(
								userId,
								safeRawText,
								safeNormalizedText,
								MentionCandidateStatus.CONFIRMED
						);

		if (histories.isEmpty()) {
			return Optional.empty();
		}
		
		Optional<UUID> rawTextMatch =
				findStableRawTextMatch(
						histories,
						safeRawText,
						blockedIds,
						currentCandidateIds
				);

		if (rawTextMatch.isPresent()) {
			return rawTextMatch;
		}

		return findStableNormalizedTextMatch(
				histories,
				safeRawText,
				safeNormalizedText,
				blockedIds
		);
	}
	
	private Optional<UUID> findStableRawTextMatch(
			List<MentionCandidate> histories,
			String rawText,
			Set<UUID> blockedPersonIds,
			Set<UUID> currentCandidatePersonIds
	) {
		if (rawText.isBlank()) {
			return Optional.empty();
		}

		Map<UUID, Set<UUID>> diaryIdsByPerson =
				new HashMap<>();

		for (MentionCandidate history : histories) {
			if (!rawText.equals(
					history.getRawText()
			)) {
				continue;
			}

			UUID personId =
					history.getMatchedPersonId();

			if (personId == null) {
				continue;
			}

			if (blockedPersonIds.contains(
					personId
			)) {
				continue;
			}

			diaryIdsByPerson
					.computeIfAbsent(
							personId,
							key -> new HashSet<>()
					)
					.add(
							history.getDiaryId()
					);
		}

		if (diaryIdsByPerson.size() != 1) {
			return Optional.empty();
		}

		Map.Entry<UUID, Set<UUID>> onlyMatch =
				diaryIdsByPerson.entrySet()
						.iterator()
						.next();
		
		UUID personId =
				onlyMatch.getKey();
		
		int historyCount =
				onlyMatch.getValue().size();
		
		/*
		 * 서로 다른 ACTIVE Diary에서 동일 rawText가 2회 이상
		 * 같은 Person으로 수렴했다면 안정 이력으로 재사용한다.
		 */
		if (historyCount
				>= PersonMatchingPolicy.RAW_TEXT_HISTORY_MIN_COUNT) {
			return Optional.of(personId);
		}
		
		/*
		 * 동일 rawText 이력이 1회뿐이면 과거 이력만으로 재사용하지 않는다.
		 * 단, 현재 매칭에서도 동일 Person이 AMBIGUOUS 후보로 나온 경우에는
		 * 과거 이력과 현재 매칭이라는 두 근거가 일치한 것으로 보고 재사용한다.
		 */
		if (historyCount == 1
				&& currentCandidatePersonIds.contains(personId)) {
			return Optional.of(personId);
		}
		
		return Optional.empty();
	}

	private Optional<UUID> findStableNormalizedTextMatch(
			List<MentionCandidate> histories,
			String rawText,
			String normalizedText,
			Set<UUID> blockedPersonIds
	) {
		if (normalizedText.isBlank()) {
			return Optional.empty();
		}

		/*
		 * 입력이 애매한 조사(은/이/도/랑/님/씨/아)를 떼야만 이 값에 도달했다면 재사용을
		 * 시도하지 않는다. PersonMatchingService EXACT 판정과 동일 기준: safeMatchCandidates만 신뢰.
		 */
		if (!personNormalizer.normalize(rawText)
				.safeMatchCandidates()
				.contains(normalizedText)) {
			return Optional.empty();
		}

		/*
		 * 일기 단위로 센다 — 한 일기 안에서 같은 표현이 여러 번 나와도 이력 1회로 세야
		 * 말이 긴 일기 하나가 안정성 점수를 부풀리지 않는다.
		 */
		Map<UUID, Set<UUID>> historyDiaryIdsByPerson =
				new HashMap<>();

		for (MentionCandidate history : histories) {
			if (!normalizedText.equals(
					history.getNormalizedText()
			)) {
				continue;
			}

			/*
			 * 과거 이력도 안전한 조사 제거로만 도달했을 때만 인정한다 —
			 * "김성은"은 애매한 조사를 떼야만 "김성"이 되므로 이력으로 세지 않는다.
			 */
			if (!personNormalizer.normalize(
					history.getRawText()
			).safeMatchCandidates().contains(
					normalizedText
			)) {
				continue;
			}

			UUID personId =
					history.getMatchedPersonId();

			if (personId == null) {
				continue;
			}

			if (blockedPersonIds.contains(
					personId
			)) {
				continue;
			}

			historyDiaryIdsByPerson
					.computeIfAbsent(
							personId,
							key -> new HashSet<>()
					)
					.add(
							history.getDiaryId()
					);
		}

		if (historyDiaryIdsByPerson.isEmpty()) {
			return Optional.empty();
		}

		List<HistoryScore> rankedHistories =
				historyDiaryIdsByPerson.entrySet()
						.stream()
						.map(
								entry ->
										new HistoryScore(
												entry.getKey(),
												entry.getValue().size()
										)
						)
						.sorted(
								Comparator
										.comparingInt(
												HistoryScore::count
										)
										.reversed()
										.thenComparing(
												score ->
														score.personId()
																.toString()
										)
						)
						.toList();

		HistoryScore first =
				rankedHistories.get(0);

		if (first.count()
				< PersonMatchingPolicy.NORMALIZED_HISTORY_MIN_COUNT) {
			return Optional.empty();
		}

		int totalCount =
				historyDiaryIdsByPerson.values()
						.stream()
						.mapToInt(
								Set::size
						)
						.sum();

		double dominanceRatio =
				(double) first.count()
						/ totalCount;

		if (dominanceRatio
				< PersonMatchingPolicy
				.NORMALIZED_HISTORY_MIN_DOMINANCE_RATIO) {
			return Optional.empty();
		}

		int secondCount =
				rankedHistories.size() >= 2
						? rankedHistories.get(1).count()
						: 0;

		int countGap =
				first.count() - secondCount;

		if (countGap
				< PersonMatchingPolicy
				.NORMALIZED_HISTORY_MIN_COUNT_GAP) {
			return Optional.empty();
		}

		return Optional.of(
				first.personId()
		);
	}

	private record HistoryScore(
			UUID personId,
			int count
	) {
	}
}