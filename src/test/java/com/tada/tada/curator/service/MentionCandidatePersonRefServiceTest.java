package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.entity.MentionCandidatePersonRef;
import com.tada.tada.curator.entity.MentionCandidateStatus;
import com.tada.tada.curator.entity.MentionEntityType;
import com.tada.tada.curator.repository.MentionCandidatePersonRefRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MentionCandidatePersonRefServiceTest {

	private MentionCandidatePersonRefRepository relationRepository;
	private MentionCandidatePersonRefService relationService;

	@BeforeEach
	void setUp() {
		relationRepository =
				Mockito.mock(
						MentionCandidatePersonRefRepository.class
				);

		relationService =
				new MentionCandidatePersonRefService(
						relationRepository
				);
	}

	@Test
	void 신규_relation_생성은_기존_relation을_조회하지_않는다() {
		UUID diaryId = UUID.randomUUID();
		UUID memoryPersonId = UUID.randomUUID();

		MentionCandidate personCandidate =
				MentionCandidate.create(
						diaryId,
						"민수",
						"민수",
						MentionEntityType.PERSON,
						MentionCandidateStatus.CONFIRMED,
						memoryPersonId
				);

		MentionCandidate placeCandidate =
				MentionCandidate.create(
						diaryId,
						"카페",
						"카페",
						MentionEntityType.PLACE,
						MentionCandidateStatus.CONFIRMED,
						null
				);

		relationService.createRelations(
				diaryId,
				placeCandidate,
				List.of(personCandidate)
		);

		verify(
				relationRepository,
				never()
		).findAllBySourceCandidateId(
				placeCandidate.getId()
		);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<MentionCandidatePersonRef>> captor =
				ArgumentCaptor.forClass(List.class);

		verify(
				relationRepository
		).saveAll(captor.capture());

		List<MentionCandidatePersonRef> savedRelations =
				captor.getValue();

		assertEquals(
				1,
				savedRelations.size()
		);

		assertEquals(
				placeCandidate.getId(),
				savedRelations.get(0)
						.getSourceCandidateId()
		);

		assertEquals(
				personCandidate.getId(),
				savedRelations.get(0)
						.getPersonCandidateId()
		);
	}
}