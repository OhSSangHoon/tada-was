package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.entity.MentionEntityType;
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
						MentionEntityType.PERSON,
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
			MentionEntityType entityType
	) {
		if (diaryId == null) {
			throw new IllegalArgumentException(
					"diaryId must not be null"
			);
		}

		if (entityType == null
				|| !entityType.isSource()) {
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

		/*
		 * PLACE/ACTIVITY 의 normalizedText 는 AI 의 의미 정규화 결과다.
		 *
		 * 앞뒤 공백이 남으면 "카페" 와 " 카페 " 가 다른 값이 되어
		 * 월간 활동·장소 통계 그룹이 갈린다. (명세 14.3)
		 * 형식만 정리하는 것이므로 의미를 바꾸지 않는다. (명세 5.2)
		 */
		String cleanedNormalizedText =
				normalizedText.strip();

		MentionCandidate candidate =
				MentionCandidate.create(
						diaryId,
						rawText,
						cleanedNormalizedText,
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