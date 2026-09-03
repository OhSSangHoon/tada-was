package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.entity.MentionCandidateStatus;
import com.tada.tada.curator.policy.PersonMatchingPolicy;
import com.tada.tada.curator.repository.MentionCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonCreationGuard {

	private final MentionCandidateRepository mentionCandidateRepository;

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
						blockedIds
				);

		if (rawTextMatch.isPresent()) {
			return rawTextMatch;
		}

		return findStableNormalizedTextMatch(
				histories,
				safeNormalizedText,
				blockedIds
		);
	}

	private Optional<UUID> findStableRawTextMatch(
			List<MentionCandidate> histories,
			String rawText,
			Set<UUID> blockedPersonIds
	) {
		if (rawText.isBlank()) {
			return Optional.empty();
		}

		Set<UUID> matchedPersonIds =
				new LinkedHashSet<>();

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

			matchedPersonIds.add(
					personId
			);
		}

		if (matchedPersonIds.size() != 1) {
			return Optional.empty();
		}

		return Optional.of(
				matchedPersonIds.iterator().next()
		);
	}

	private Optional<UUID> findStableNormalizedTextMatch(
			List<MentionCandidate> histories,
			String normalizedText,
			Set<UUID> blockedPersonIds
	) {
		if (normalizedText.isBlank()) {
			return Optional.empty();
		}

		Map<UUID, Integer> historyCounts =
				new HashMap<>();

		for (MentionCandidate history : histories) {
			if (!normalizedText.equals(
					history.getNormalizedText()
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

			historyCounts.merge(
					personId,
					1,
					Integer::sum
			);
		}

		if (historyCounts.isEmpty()) {
			return Optional.empty();
		}

		List<HistoryScore> rankedHistories =
				historyCounts.entrySet()
						.stream()
						.map(
								entry ->
										new HistoryScore(
												entry.getKey(),
												entry.getValue()
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
				historyCounts.values()
						.stream()
						.mapToInt(
								Integer::intValue
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