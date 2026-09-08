package com.tada.tada.curator.service;

import com.tada.tada.curator.repository.DiaryPersonRepository;
import com.tada.tada.curator.repository.MentionCandidateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CuratorCleanupServiceTest {

	private MentionCandidateRepository mentionCandidateRepository;
	private DiaryPersonRepository diaryPersonRepository;
	private CuratorCleanupService curatorCleanupService;

	@BeforeEach
	void setUp() {
		mentionCandidateRepository =
				Mockito.mock(MentionCandidateRepository.class);

		diaryPersonRepository =
				Mockito.mock(DiaryPersonRepository.class);

		curatorCleanupService =
				new CuratorCleanupService(
						mentionCandidateRepository,
						diaryPersonRepository
				);
	}

	@Test
	void Diary_종속_Curator_데이터를_모두_삭제한다() {
		UUID diaryId = UUID.randomUUID();

		curatorCleanupService.deleteByDiaryId(diaryId);

		InOrder order =
				inOrder(
						mentionCandidateRepository,
						diaryPersonRepository
				);

		order.verify(mentionCandidateRepository)
				.deleteByDiaryId(diaryId);

		order.verify(diaryPersonRepository)
				.deleteByDiaryId(diaryId);
	}

	/*
	 * mention_candidate_person_ref 는 Candidate 삭제의
	 * FK ON DELETE CASCADE 로 정리된다.
	 * 서비스에서 같은 Relation 을 이중 삭제하지 않는다.
	 */
	@Test
	void Relation을_서비스에서_따로_삭제하지_않는다() {
		curatorCleanupService.deleteByDiaryId(UUID.randomUUID());

		verify(mentionCandidateRepository, never())
				.deleteAll();

		verify(diaryPersonRepository, never())
				.deleteAll();
	}

	@Test
	void diaryId가_null이면_거부한다() {
		assertThrows(
				IllegalArgumentException.class,
				() -> curatorCleanupService.deleteByDiaryId(null)
		);

		verify(mentionCandidateRepository, never())
				.deleteByDiaryId(Mockito.any());

		verify(diaryPersonRepository, never())
				.deleteByDiaryId(Mockito.any());
	}
}
