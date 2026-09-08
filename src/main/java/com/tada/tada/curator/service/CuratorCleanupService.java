package com.tada.tada.curator.service;

import com.tada.tada.curator.repository.DiaryPersonRepository;
import com.tada.tada.curator.repository.MentionCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/*
 * Diary 영구삭제 시 Curator 종속 데이터를 물리 삭제하는 facade.
 *
 * 호출자는 Diary 도메인의 permanentlyDeleteDiary() 다.
 * 자식(Curator) 을 먼저 지우고 부모(Diary) 를 지우는 순서를 지켜야 한다.
 * diary_person 과 mention_candidate 의 diary_id FK 는 NO ACTION 이라
 * Diary 를 먼저 지우면 FK 오류가 난다.
 *
 * 이 서비스는 통계 조정 서비스가 아니다.
 * Trash 시점에 이미 통계에서 제외됐으므로
 * 영구삭제 단계에서 mention_count 를 다시 조정하지 않는다.
 * 두 번 반영하면 값이 틀어진다.
 *
 * 금지: @Transactional(propagation = REQUIRES_NEW)
 * 별도 트랜잭션을 열면 Diary 삭제가 실패해도 Curator 데이터만 사라져
 * 영구삭제 전체 rollback 이 깨진다.
 * 반드시 상위 영구삭제 트랜잭션에 참여한다.
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
		 * mention_candidate_person_ref 는 Candidate 삭제의
		 * FK ON DELETE CASCADE 로 정리된다. 별도로 지우지 않는다.
		 */
		mentionCandidateRepository.deleteByDiaryId(
				diaryId
		);

		diaryPersonRepository.deleteByDiaryId(
				diaryId
		);
	}
}
