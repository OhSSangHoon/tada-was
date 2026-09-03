package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.MemoryPerson;
import com.tada.tada.curator.entity.PersonAlias;
import com.tada.tada.curator.model.PersonMatchResult;
import com.tada.tada.curator.model.PersonNormalization;
import com.tada.tada.curator.policy.PersonMatchingPolicy;
import com.tada.tada.curator.repository.MemoryPersonRepository;
import com.tada.tada.curator.repository.PersonAliasRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonMatchingService {

	private static final int AMBIGUOUS_CANDIDATE_LIMIT = 3;

	private final PersonNormalizer personNormalizer;
	private final MemoryPersonRepository memoryPersonRepository;
	private final PersonAliasRepository personAliasRepository;

	public PersonMatchResult match(
			UUID userId,
			String rawText
	) {
		return match(
				userId,
				rawText,
				Set.of()
		);
	}

	public PersonMatchResult match(
			UUID userId,
			String rawText,
			Set<UUID> blockedPersonIds
	) {
		PersonNormalization normalization =
				personNormalizer.normalize(rawText);

		if (normalization.strongMatchCandidates().isEmpty()) {
			return PersonMatchResult.newPerson();
		}

		Set<UUID> blockedIds =
				blockedPersonIds == null
						? Set.of()
						: Set.copyOf(blockedPersonIds);

		PersonMatchResult exactResult =
				findExactResult(
						userId,
						normalization.strongMatchCandidates(),
						blockedIds
				);

		if (exactResult != null) {
			return exactResult;
		}

		return findSimilarityResult(
				userId,
				normalization,
				blockedIds
		);
	}

	private PersonMatchResult findExactResult(
			UUID userId,
			List<String> strongMatchCandidates,
			Set<UUID> blockedPersonIds
	) {
		List<MemoryPerson> persons =
				memoryPersonRepository
						.findAllByUserIdAndDisplayNameIn(
								userId,
								strongMatchCandidates
						);

		List<PersonAlias> aliases =
				personAliasRepository
						.findAllByOwnerUserIdAndNormalizedTextIn(
								userId,
								strongMatchCandidates
						);

		Set<UUID> matchedPersonIds =
				new LinkedHashSet<>();

		for (MemoryPerson person : persons) {
			if (blockedPersonIds.contains(
					person.getId()
			)) {
				continue;
			}

			if (strongMatchCandidates.contains(
					person.getDisplayName()
			)) {
				matchedPersonIds.add(
						person.getId()
				);
			}
		}

		for (PersonAlias alias : aliases) {
			if (blockedPersonIds.contains(
					alias.getPersonId()
			)) {
				continue;
			}

			if (strongMatchCandidates.contains(
					alias.getNormalizedText()
			)) {
				matchedPersonIds.add(
						alias.getPersonId()
				);
			}
		}

		if (matchedPersonIds.size() == 1) {
			return PersonMatchResult.exact(
					matchedPersonIds.iterator().next()
			);
		}

		if (matchedPersonIds.size() > 1) {
			return PersonMatchResult.ambiguous(
					sortPersonIds(
							matchedPersonIds
					)
			);
		}

		return null;
	}

	private PersonMatchResult findSimilarityResult(
			UUID userId,
			PersonNormalization normalization,
			Set<UUID> blockedPersonIds
	) {
		List<MemoryPerson> persons =
				memoryPersonRepository.findAllByUserId(
						userId
				);

		List<PersonAlias> aliases =
				personAliasRepository.findAllByOwnerUserId(
						userId
				);

		Map<UUID, Integer> scores =
				new HashMap<>();

		addWeakCandidateScores(
				normalization.weakMatchCandidates(),
				persons,
				aliases,
				blockedPersonIds,
				scores
		);

		addEditDistanceScores(
				normalization.normalizedText(),
				persons,
				aliases,
				blockedPersonIds,
				scores
		);

		List<PersonScore> rankedCandidates =
				rankCandidates(scores);

		if (rankedCandidates.isEmpty()) {
			return PersonMatchResult.newPerson();
		}

		PersonScore first =
				rankedCandidates.get(0);

		int secondScore =
				rankedCandidates.size() >= 2
						? rankedCandidates.get(1).score()
						: 0;

		int scoreGap =
				first.score() - secondScore;

		if (first.score()
				>= PersonMatchingPolicy.SIMILAR_MIN_SCORE
				&& scoreGap
				>= PersonMatchingPolicy.SIMILAR_MIN_SCORE_GAP) {

			return PersonMatchResult.similar(
					first.personId()
			);
		}

		return PersonMatchResult.ambiguous(
				rankedCandidates.stream()
						.limit(
								AMBIGUOUS_CANDIDATE_LIMIT
						)
						.map(
								PersonScore::personId
						)
						.toList()
		);
	}

	private void addWeakCandidateScores(
			List<String> weakMatchCandidates,
			List<MemoryPerson> persons,
			List<PersonAlias> aliases,
			Set<UUID> blockedPersonIds,
			Map<UUID, Integer> scores
	) {
		if (weakMatchCandidates.isEmpty()) {
			return;
		}

		Set<UUID> matchedPersonIds =
				new HashSet<>();

		for (MemoryPerson person : persons) {
			if (blockedPersonIds.contains(
					person.getId()
			)) {
				continue;
			}

			if (weakMatchCandidates.contains(
					person.getDisplayName()
			)) {
				matchedPersonIds.add(
						person.getId()
				);
			}
		}

		for (PersonAlias alias : aliases) {
			if (blockedPersonIds.contains(
					alias.getPersonId()
			)) {
				continue;
			}

			if (weakMatchCandidates.contains(
					alias.getNormalizedText()
			)) {
				matchedPersonIds.add(
						alias.getPersonId()
				);
			}
		}

		for (UUID personId : matchedPersonIds) {
			addScore(
					scores,
					personId,
					PersonMatchingPolicy.STRONG_SCORE
			);
		}
	}

	private void addEditDistanceScores(
			String normalizedText,
			List<MemoryPerson> persons,
			List<PersonAlias> aliases,
			Set<UUID> blockedPersonIds,
			Map<UUID, Integer> scores
	) {
		if (normalizedText == null
				|| normalizedText.isBlank()) {
			return;
		}

		Map<UUID, Integer> bestEditScores =
				new HashMap<>();

		for (MemoryPerson person : persons) {
			if (blockedPersonIds.contains(
					person.getId()
			)) {
				continue;
			}

			updateBestEditScore(
					bestEditScores,
					person.getId(),
					normalizedText,
					person.getDisplayName()
			);
		}

		for (PersonAlias alias : aliases) {
			if (blockedPersonIds.contains(
					alias.getPersonId()
			)) {
				continue;
			}

			updateBestEditScore(
					bestEditScores,
					alias.getPersonId(),
					normalizedText,
					alias.getNormalizedText()
			);
		}

		for (Map.Entry<UUID, Integer> entry
				: bestEditScores.entrySet()) {

			addScore(
					scores,
					entry.getKey(),
					entry.getValue()
			);
		}
	}

	private void updateBestEditScore(
			Map<UUID, Integer> bestEditScores,
			UUID personId,
			String sourceText,
			String targetText
	) {
		if (targetText == null
				|| targetText.isBlank()) {
			return;
		}

		if (sourceText.equals(
				targetText
		)) {
			return;
		}

		int distance =
				calculateEditDistance(
						sourceText,
						targetText
				);

		if (distance <= 0
				|| distance
				> PersonMatchingPolicy.MAX_EDIT_DISTANCE) {
			return;
		}

		int shorterLength =
				Math.min(
						sourceText.length(),
						targetText.length()
				);

		int score =
				shorterLength
						>= PersonMatchingPolicy.LONG_NAME_MIN_LENGTH
						? PersonMatchingPolicy.STRONG_SCORE
						: PersonMatchingPolicy.WEAK_SCORE;

		bestEditScores.merge(
				personId,
				score,
				Math::max
		);
	}

	private void addScore(
			Map<UUID, Integer> scores,
			UUID personId,
			int score
	) {
		scores.merge(
				personId,
				score,
				Integer::sum
		);
	}

	private List<PersonScore> rankCandidates(
			Map<UUID, Integer> scores
	) {
		List<PersonScore> rankedCandidates =
				new ArrayList<>();

		for (Map.Entry<UUID, Integer> entry
				: scores.entrySet()) {

			rankedCandidates.add(
					new PersonScore(
							entry.getKey(),
							entry.getValue()
					)
			);
		}

		rankedCandidates.sort(
				Comparator
						.comparingInt(
								PersonScore::score
						)
						.reversed()
						.thenComparing(
								score ->
										score.personId().toString()
						)
		);

		return rankedCandidates;
	}

	private int calculateEditDistance(
			String left,
			String right
	) {
		if (left.equals(right)) {
			return 0;
		}

		if (Math.abs(
				left.length() - right.length()
		) > PersonMatchingPolicy.MAX_EDIT_DISTANCE) {

			return PersonMatchingPolicy.MAX_EDIT_DISTANCE + 1;
		}

		int[] previous =
				new int[right.length() + 1];

		for (int j = 0; j <= right.length(); j++) {
			previous[j] = j;
		}

		for (int i = 1; i <= left.length(); i++) {
			int[] current =
					new int[right.length() + 1];

			current[0] = i;

			for (int j = 1; j <= right.length(); j++) {
				int cost =
						left.charAt(i - 1)
								== right.charAt(j - 1)
								? 0
								: 1;

				current[j] =
						Math.min(
								Math.min(
										current[j - 1] + 1,
										previous[j] + 1
								),
								previous[j - 1] + cost
						);
			}

			previous = current;
		}

		return previous[right.length()];
	}

	private List<UUID> sortPersonIds(
			Collection<UUID> personIds
	) {
		return personIds.stream()
				.sorted(
						Comparator.comparing(
								UUID::toString
						)
				)
				.toList();
	}

	private record PersonScore(
			UUID personId,
			int score
	) {
	}
}