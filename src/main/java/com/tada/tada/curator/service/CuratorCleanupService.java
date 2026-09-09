package com.tada.tada.curator.service;

import com.tada.tada.curator.repository.DiaryPersonRepository;
import com.tada.tada.curator.repository.MentionCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/*
 * Diary 영구삭제 시 Curator 종속 데이터를 물리 삭제하는 facade다.
 * 자식(Curator)을 먼저 지운다 — diary_id FK가 NO ACTION이라 순서를 지키지 않으면 FK 오류.
 * 통계는 Trash 시점에 이미 제외됐으므로 여기서 mention_count를 다시 조정하지 않는다 (이중 반영 방지).
 * 금지: REQUIRES_NEW — 별도 트랜잭션이면 Diary 삭제 실패 시에도 Curator 데이터만 사라져 rollback이 깨진다.
 */
@Service
@RequiredArgsConstructor
public class CuratorCleanupService {

	private final MentionCandidateRepository mentionCandidateRepository;
	private final DiaryPersonRepository diaryPersonRepository;

	@Transactional
	public void deleteByDiaryId(
			UUID diaryId
	) {
		if (diaryId == null) {
			throw new IllegalArgumentException(
					"diaryId must not be null"
			);
		}

		/*
		 * mention_candidate_person_ref는 FK ON DELETE CASCADE로 정리되므로 별도로 지우지 않는다.
		 */
		mentionCandidateRepository.deleteByDiaryId(
				diaryId
		);

		diaryPersonRepository.deleteByDiaryId(
				diaryId
		);
	}
}
