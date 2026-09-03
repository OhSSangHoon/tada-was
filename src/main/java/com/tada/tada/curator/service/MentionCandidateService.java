package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.entity.MentionCandidateStatus;
import com.tada.tada.curator.model.PersonNormalization;
import com.tada.tada.curator.repository.MentionCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MentionCandidateService {

	private static final String PERSON = "PERSON";
	private static final String PLACE = "PLACE";
	private static final String ACTIVITY = "ACTIVITY";

	private final MentionCandidateRepository mentionCandidateRepository;
	private final PersonResolverService personResolverService;
	private final PersonNormalizer personNormalizer;

	public MentionCandidate save(
			MentionCandidate candidate
	) {
		return mentionCandidateRepository.save(candidate);
	}

	public boolean hasCandidates(
			UUID diaryId
	) {
		if (diaryId == null) {
			throw new IllegalArgumentException(
					"diaryId must not be null"
			);
		}

		return mentionCandidateRepository.existsByDiaryId(
				diaryId
		);
	}

	public MentionCandidate createPersonCandidate(
			UUID diaryId,
			UUID userId,
			String rawText
	) {
		return createPersonCandidate(
				diaryId,
				userId,
				rawText,
				Set.of()
		);
	}

	public MentionCandidate createPersonCandidate(
			UUID diaryId,
			UUID userId,
			String rawText,
			Set<UUID> blockedPersonIds
	) {
		if (diaryId == null) {
			throw new IllegalArgumentException(
					"diaryId must not be null"
			);
		}

		if (userId == null) {
			throw new IllegalArgumentException(
					"userId must not be null"
			);
		}

		PersonNormalization normalization =
				personNormalizer.normalize(rawText);

		if (normalization.normalizedText().isBlank()) {
			throw new IllegalArgumentException(
					"person normalizedText must not be blank"
			);
		}

		UUID matchedPersonId =
				personResolverService.resolve(
						userId,
						rawText,
						blockedPersonIds
				);

		MentionCandidate candidate =
				MentionCandidate.create(
						diaryId,
						rawText,
						normalization.normalizedText(),
						PERSON,
						MentionCandidateStatus.CONFIRMED,
						matchedPersonId
				);

		return mentionCandidateRepository.save(
				candidate
		);
	}

	public MentionCandidate createNonPersonCandidate(
			UUID diaryId,
			String rawText,
			String normalizedText,
			String entityType
	) {
		if (diaryId == null) {
			throw new IllegalArgumentException(
					"diaryId must not be null"
			);
		}

		if (!PLACE.equals(entityType)
				&& !ACTIVITY.equals(entityType)) {
			throw new IllegalArgumentException(
					"entityType must be PLACE or ACTIVITY"
			);
		}

		if (rawText == null
				|| rawText.isBlank()) {
			throw new IllegalArgumentException(
					"rawText must not be blank"
			);
		}

		if (normalizedText == null
				|| normalizedText.isBlank()) {
			throw new IllegalArgumentException(
					"normalizedText must not be blank"
			);
		}

		MentionCandidate candidate =
				MentionCandidate.create(
						diaryId,
						rawText,
						normalizedText,
						entityType,
						MentionCandidateStatus.CONFIRMED,
						null
				);

		return mentionCandidateRepository.save(
				candidate
		);
	}

	public List<MentionCandidate> findAllByDiaryId(
			UUID diaryId
	) {
		if (diaryId == null) {
			throw new IllegalArgumentException(
					"diaryId must not be null"
			);
		}

		return mentionCandidateRepository
				.findAllByDiaryId(diaryId);
	}
}