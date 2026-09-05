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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonMatchingService {

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
		if (userId == null) {
			throw new IllegalArgumentException(
					"userId must not be null"
			);
		}

		PersonNormalization normalization =
				personNormalizer.normalize(
						rawText
				);

		if (normalization
				.strongMatchCandidates()
				.isEmpty()) {

			return PersonMatchResult.newPerson();
		}

		Set<UUID> blockedIds =
				blockedPersonIds == null
						? Set.of()
						: Set.copyOf(
						blockedPersonIds
				);

		PersonMatchResult exactResult =
				findExactResult(
						userId,
						normalization
								.strongMatchCandidates(),
						blockedIds
				);

		if (exactResult != null) {
			return exactResult;
		}

		List<MemoryPerson> persons =
				memoryPersonRepository
						.findAllByUserId(
								userId
						);

		List<PersonAlias> aliases =
				personAliasRepository
						.findAllByOwnerUserId(
								userId
						);

		PersonMatchResult normalizedExactResult =
				findNormalizedExactResult(
						normalization
								.strongMatchCandidates(),
						persons,
						aliases,
						blockedIds
				);

		if (normalizedExactResult != null) {
			return normalizedExactResult;
		}

		return findSimilarityResult(
				normalization,
				persons,
				aliases,
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

		/*
		 * strongMatchCandidates는
		 *
		 * 1. 원문
		 * 2. 조사 제거 결과
		 *
		 * 순서로 들어온다.
		 *
		 * 후보를 합쳐서 한 번에 판단하면
		 * 원문 exact가 존재하는데 조사 제거 결과와 충돌했을 때
		 * AMBIGUOUS가 되어 버린다.
		 *
		 * 따라서 후보 우선순위를 유지하면서
		 * 단계별로 exact를 판단한다.
		 */
		for (String candidate
				: strongMatchCandidates) {

			Set<UUID> matchedPersonIds =
					new LinkedHashSet<>();

			for (MemoryPerson person : persons) {
				if (!blockedPersonIds.contains(
						person.getId()
				)
						&& candidate.equals(
						person.getDisplayName()
				)) {

					matchedPersonIds.add(
							person.getId()
					);
				}
			}

			for (PersonAlias alias : aliases) {
				if (!blockedPersonIds.contains(
						alias.getPersonId()
				)
						&& candidate.equals(
						alias.getNormalizedText()
				)) {

					matchedPersonIds.add(
							alias.getPersonId()
					);
				}
			}

			if (matchedPersonIds.size() == 1) {
				return PersonMatchResult.exact(
						matchedPersonIds
								.iterator()
								.next()
				);
			}

			if (matchedPersonIds.size() > 1) {
				return PersonMatchResult.ambiguous(
						sortPersonIds(
								matchedPersonIds
						)
				);
			}
		}

		return null;
	}

	/*
	 * displayName 자체는 다르지만
	 * 정규화하면 같은 이름으로 수렴하는 기존 인물을 찾는다.
	 *
	 * 신규 인물의 displayName 은 이름 훼손을 막기 위해
	 * 애매한 접미사를 보존한다. ("김성은", "가을이")
	 *
	 * 그래서 같은 사람이 나중에 다른 조사로 등장하면
	 * displayName 직접 비교만으로는 다시 연결되지 않는다.
	 *
	 * memory_person 에 정규화 컬럼을 추가하지 않고
	 * 조회 시점에 정규화해 비교한다.
	 *
	 * 원문 exact 와 조사 제거 exact 가 모두 실패한 뒤에만 시도하므로
	 * 단계별 우선순위는 그대로 유지된다.
	 */
	private PersonMatchResult findNormalizedExactResult(
			List<String> strongMatchCandidates,
			List<MemoryPerson> persons,
			List<PersonAlias> aliases,
			Set<UUID> blockedPersonIds
	) {
		Map<UUID, Set<String>> normalizedNamesByPerson =
				new LinkedHashMap<>();

		for (MemoryPerson person : persons) {
			if (blockedPersonIds.contains(
					person.getId()
			)) {
				continue;
			}

			addNormalizedName(
					normalizedNamesByPerson,
					person.getId(),
					person.getDisplayName()
			);
		}

		for (PersonAlias alias : aliases) {
			if (blockedPersonIds.contains(
					alias.getPersonId()
			)) {
				continue;
			}

			addNormalizedName(
					normalizedNamesByPerson,
					alias.getPersonId(),
					alias.getNormalizedText()
			);
		}

		for (String candidate : strongMatchCandidates) {
			Set<UUID> matchedPersonIds =
					new LinkedHashSet<>();

			for (Map.Entry<UUID, Set<String>> entry
					: normalizedNamesByPerson.entrySet()) {

				if (entry.getValue().contains(
						candidate
				)) {
					matchedPersonIds.add(
							entry.getKey()
					);
				}
			}

			if (matchedPersonIds.size() == 1) {
				return PersonMatchResult.exact(
						matchedPersonIds
								.iterator()
								.next()
				);
			}

			if (matchedPersonIds.size() > 1) {
				return PersonMatchResult.ambiguous(
						sortPersonIds(matchedPersonIds)
				);
			}
		}

		return null;
	}

	private void addNormalizedName(
			Map<UUID, Set<String>> normalizedNamesByPerson,
			UUID personId,
			String name
	) {
		if (name == null || name.isBlank()) {
			return;
		}

		String normalizedName =
				personNormalizer
						.normalize(name)
						.normalizedText();

		if (normalizedName.isBlank()) {
			return;
		}

		normalizedNamesByPerson
				.computeIfAbsent(
						personId,
						key -> new LinkedHashSet<>()
				)
				.add(normalizedName);
	}

	private PersonMatchResult findSimilarityResult(
			PersonNormalization normalization,
			List<MemoryPerson> persons,
			List<PersonAlias> aliases,
			Set<UUID> blockedPersonIds
	) {
		Map<UUID, Integer> scores =
				new HashMap<>();

		addWeakCandidateScores(
				normalization,
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
						? rankedCandidates
						.get(1)
						.score()
						: 0;

		int scoreGap =
				first.score() - secondScore;

		if (first.score()
				>= PersonMatchingPolicy
				.SIMILAR_MIN_SCORE

				&& scoreGap
				>= PersonMatchingPolicy
				.SIMILAR_MIN_SCORE_GAP) {

			return PersonMatchResult.similar(
					first.personId()
			);
		}

		/*
		 * 후보 목록을 자르지 않는다.
		 *
		 * PersonResolverService 가 CreationGuard 의 안정 이력이
		 * 이 목록에 있는지로 재사용 여부를 판단하므로,
		 * 상위 N개만 남기면 근거 있는 이력이 조용히 버려진다.
		 */
		return PersonMatchResult.ambiguous(
				rankedCandidates.stream()
						.map(
								PersonScore::personId
						)
						.toList()
		);
	}

	/*
	 * 성 포함/생략 변형을 점수로 반영한다. (명세 9.3 의 STRONG 근거)
	 *
	 *   입력 "김민혁" + 기존 인물 "민혁"
	 *   입력 "민혁"   + 기존 인물 "김민혁"
	 *
	 * 단독으로는 SIMILAR 임계값에 못 미치므로 자동 연결되지 않고,
	 * 다른 근거와 결합할 때만 SIMILAR 이 된다.
	 */
	private void addWeakCandidateScores(
			PersonNormalization normalization,
			List<MemoryPerson> persons,
			List<PersonAlias> aliases,
			Set<UUID> blockedPersonIds,
			Map<UUID, Integer> scores
	) {
		Set<UUID> matchedPersonIds =
				new LinkedHashSet<>();

		for (MemoryPerson person : persons) {
			if (blockedPersonIds.contains(
					person.getId()
			)) {
				continue;
			}

			if (matchesSurnameVariant(
					normalization,
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

			if (matchesSurnameVariant(
					normalization,
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

	private boolean matchesSurnameVariant(
			PersonNormalization normalization,
			String targetName
	) {
		if (targetName == null
				|| targetName.isBlank()) {
			return false;
		}

		String normalizedTarget =
				personNormalizer
						.normalize(targetName)
						.normalizedText();

		if (normalizedTarget.isBlank()) {
			return false;
		}

		/*
		 * 입력에서 성을 뗀 형태가 기존 이름과 같은 경우.
		 * 예: 입력 "김민혁" -> "민혁", 기존 인물 "민혁"
		 */
		if (normalization.weakMatchCandidates()
				.contains(normalizedTarget)) {

			return true;
		}

		/*
		 * 기존 이름에서 성을 뗀 형태가 입력과 같은 경우.
		 * 예: 입력 "민혁", 기존 인물 "김민혁" -> "민혁"
		 */
		String targetWithoutSurname =
				personNormalizer.removeSurname(
						normalizedTarget
				);

		return !targetWithoutSurname.equals(normalizedTarget)
				&& targetWithoutSurname.equals(
				normalization.normalizedText()
		);
	}

	private void addEditDistanceScores(
			String normalizedText,
			List<MemoryPerson> persons,
			List<PersonAlias> aliases,
			Set<UUID> blockedPersonIds,
			Map<UUID, Integer> scores
	) {
		if (normalizedText == null
				|| normalizedText.isBlank()
				|| normalizedText.length() < 2) {
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
				|| targetText.isBlank()
				|| targetText.length() < 2) {
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
				> PersonMatchingPolicy
				.MAX_EDIT_DISTANCE) {

			return;
		}

		int shorterLength =
				Math.min(
						sourceText.length(),
						targetText.length()
				);

		int score =
				shorterLength
						>= PersonMatchingPolicy
						.LONG_NAME_MIN_LENGTH

						? PersonMatchingPolicy
						.STRONG_SCORE

						: PersonMatchingPolicy
						.WEAK_SCORE;

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
										score
												.personId()
												.toString()
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
				left.length()
						- right.length()
		) > PersonMatchingPolicy
				.MAX_EDIT_DISTANCE) {

			return PersonMatchingPolicy
					.MAX_EDIT_DISTANCE + 1;
		}

		int[] previous =
				new int[right.length() + 1];

		for (int j = 0;
			 j <= right.length();
			 j++) {

			previous[j] = j;
		}

		for (int i = 1;
			 i <= left.length();
			 i++) {

			int[] current =
					new int[right.length() + 1];

			current[0] = i;

			for (int j = 1;
				 j <= right.length();
				 j++) {

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
								previous[j - 1]
										+ cost
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