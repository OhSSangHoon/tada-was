package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.MemoryPerson;
import com.tada.tada.curator.entity.PersonAlias;
import com.tada.tada.curator.repository.MemoryPersonRepository;
import com.tada.tada.curator.repository.PersonAliasRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonMatchingService {
	
	private static final int SIMILAR_MAX_DISTANCE = 1;
	private static final int SIMILAR_LIMIT = 3;
	
	private final PersonNormalizer personNormalizer;
	private final MemoryPersonRepository memoryPersonRepository;
	private final PersonAliasRepository personAliasRepository;
	
	public PersonMatchResult match(UUID userId, String rawText) {
		List<String> candidates =
				personNormalizer.normalizeCandidates(rawText);
		
		if (candidates.isEmpty()) {
			return PersonMatchResult.newPerson();
		}
		
		List<MemoryPerson> persons =
				memoryPersonRepository.findAllByUserIdAndDisplayNameIn(
						userId,
						candidates
				);
		
		List<PersonAlias> aliases =
				personAliasRepository.findAllByOwnerUserIdAndNormalizedTextIn(
						userId,
						candidates
				);
		
		for (String candidate : candidates) {
			Set<UUID> matchedPersonIds = findMatchedPersonIds(
					candidate,
					persons,
					aliases
			);
			
			if (matchedPersonIds.size() == 1) {
				return PersonMatchResult.exact(
						matchedPersonIds.iterator().next()
				);
			}
			
			if (matchedPersonIds.size() > 1) {
				return PersonMatchResult.ambiguous(
						sortPersonIds(matchedPersonIds)
				);
			}
		}
		
		String normalizedName =
				personNormalizer.normalizeName(rawText);
		
		List<UUID> similarPersonIds =
				findSimilarPersonIds(userId, normalizedName);
		
		if (!similarPersonIds.isEmpty()) {
			return PersonMatchResult.similar(similarPersonIds);
		}
		
		return PersonMatchResult.newPerson();
	}
	
	private Set<UUID> findMatchedPersonIds(
			String candidate,
			List<MemoryPerson> persons,
			List<PersonAlias> aliases
	) {
		Set<UUID> personIds = new LinkedHashSet<>();
		
		for (MemoryPerson person : persons) {
			if (candidate.equals(person.getDisplayName())) {
				personIds.add(person.getId());
			}
		}
		
		for (PersonAlias alias : aliases) {
			if (candidate.equals(alias.getNormalizedText())) {
				personIds.add(alias.getPersonId());
			}
		}
		
		return personIds;
	}
	
	private List<UUID> findSimilarPersonIds(
			UUID userId,
			String normalizedName
	) {
		if (normalizedName.length() < 2) {
			return List.of();
		}
		
		List<MemoryPerson> persons =
				memoryPersonRepository.findAllByUserId(userId);
		
		List<PersonAlias> aliases =
				personAliasRepository.findAllByOwnerUserId(userId);
		
		Map<UUID, Integer> bestDistances = new HashMap<>();
		
		for (MemoryPerson person : persons) {
			updateBestDistance(
					bestDistances,
					person.getId(),
					person.getDisplayName(),
					normalizedName
			);
		}
		
		for (PersonAlias alias : aliases) {
			updateBestDistance(
					bestDistances,
					alias.getPersonId(),
					alias.getNormalizedText(),
					normalizedName
			);
		}
		
		return bestDistances.entrySet()
				.stream()
				.sorted(
						Map.Entry.<UUID, Integer>comparingByValue()
								.thenComparing(
										entry -> entry.getKey().toString()
								)
				)
				.limit(SIMILAR_LIMIT)
				.map(Map.Entry::getKey)
				.toList();
	}
	
	private void updateBestDistance(
			Map<UUID, Integer> bestDistances,
			UUID personId,
			String targetText,
			String normalizedName
	) {
		if (targetText == null || targetText.isBlank()) {
			return;
		}
		
		if (targetText.length() < 2) {
			return;
		}
		
		int distance = calculateEditDistance(
				normalizedName,
				targetText
		);
		
		if (distance > 0 && distance <= SIMILAR_MAX_DISTANCE) {
			bestDistances.merge(
					personId,
					distance,
					Math::min
			);
		}
	}
	
	private int calculateEditDistance(
			String left,
			String right
	) {
		if (left.equals(right)) {
			return 0;
		}
		
		if (Math.abs(left.length() - right.length())
				> SIMILAR_MAX_DISTANCE) {
			return SIMILAR_MAX_DISTANCE + 1;
		}
		
		int[] previous = new int[right.length() + 1];
		
		for (int j = 0; j <= right.length(); j++) {
			previous[j] = j;
		}
		
		for (int i = 1; i <= left.length(); i++) {
			int[] current = new int[right.length() + 1];
			current[0] = i;
			
			for (int j = 1; j <= right.length(); j++) {
				int cost =
						left.charAt(i - 1) == right.charAt(j - 1)
								? 0
								: 1;
				
				current[j] = Math.min(
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
	
	private List<UUID> sortPersonIds(Set<UUID> personIds) {
		return personIds.stream()
				.sorted(Comparator.comparing(UUID::toString))
				.toList();
	}
}