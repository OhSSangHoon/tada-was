package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.entity.MentionCandidateStatus;
import com.tada.tada.curator.entity.MentionEntityType;
import com.tada.tada.curator.repository.MentionCandidateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MentionCandidateServiceTest {

	private MentionCandidateRepository mentionCandidateRepository;
	private PersonResolverService personResolverService;

	private MentionCandidateService mentionCandidateService;

	@BeforeEach
	void setUp() {
		mentionCandidateRepository =
				Mockito.mock(MentionCandidateRepository.class);

		personResolverService =
				Mockito.mock(PersonResolverService.class);

		mentionCandidateService =
				new MentionCandidateService(
						mentionCandidateRepository,
						personResolverService,
						new PersonNormalizer()
				);

		when(
				mentionCandidateRepository.save(
						any(MentionCandidate.class)
				)
		).thenAnswer(
				invocation ->
						invocation.getArgument(0)
		);
	}

	@Test
	void person후보는_confirmed와_matchedPersonId를_가지고_저장한다() {
		UUID diaryId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		when(
				personResolverService.resolve(
						userId,
						"한영이와",
						Set.of()
				)
		).thenReturn(personId);

		MentionCandidate result =
				mentionCandidateService
						.createPersonCandidate(
								diaryId,
								userId,
								"한영이와"
						);

		assertEquals(
				diaryId,
				result.getDiaryId()
		);

		assertEquals(
				"한영이와",
				result.getRawText()
		);

		assertEquals(
				"한영",
				result.getNormalizedText()
		);

		assertEquals(
				MentionEntityType.PERSON,
				result.getEntityType()
		);

		assertEquals(
				MentionCandidateStatus.CONFIRMED,
				result.getStatus()
		);

		assertEquals(
				personId,
				result.getMatchedPersonId()
		);

		verify(
				mentionCandidateRepository
		).save(result);
	}

	@Test
	void blockedPersonIds를_personResolver에_그대로_전달한다() {
		UUID diaryId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		UUID blockedPersonId =
				UUID.randomUUID();

		UUID resolvedPersonId =
				UUID.randomUUID();

		Set<UUID> blockedPersonIds =
				Set.of(blockedPersonId);

		when(
				personResolverService.resolve(
						userId,
						"민수",
						blockedPersonIds
				)
		).thenReturn(resolvedPersonId);

		MentionCandidate result =
				mentionCandidateService
						.createPersonCandidate(
								diaryId,
								userId,
								"민수",
								blockedPersonIds
						);

		assertEquals(
				resolvedPersonId,
				result.getMatchedPersonId()
		);

		verify(
				personResolverService
		).resolve(
				userId,
				"민수",
				blockedPersonIds
		);
	}

	@Test
	void place후보는_confirmed이지만_matchedPersonId는_null이다() {
		UUID diaryId = UUID.randomUUID();

		MentionCandidate result =
				mentionCandidateService
						.createNonPersonCandidate(
								diaryId,
								"광안리에서",
								"광안리",
								MentionEntityType.PLACE
						);

		assertEquals(
				MentionEntityType.PLACE,
				result.getEntityType()
		);

		assertEquals(
				MentionCandidateStatus.CONFIRMED,
				result.getStatus()
		);

		assertNull(
				result.getMatchedPersonId()
		);
	}

	@Test
	void activity후보도_matchedPersonId는_null이다() {
		UUID diaryId = UUID.randomUUID();

		MentionCandidate result =
				mentionCandidateService
						.createNonPersonCandidate(
								diaryId,
								"농구를 했다",
								"농구",
								MentionEntityType.ACTIVITY
						);

		assertEquals(
				MentionEntityType.ACTIVITY,
				result.getEntityType()
		);

		assertNull(
				result.getMatchedPersonId()
		);
	}

	@Test
	void person을_nonPerson생성메서드로_만들수없다() {
		assertThrows(
				IllegalArgumentException.class,
				() ->
						mentionCandidateService
								.createNonPersonCandidate(
										UUID.randomUUID(),
										"민수",
										"민수",
										MentionEntityType.PERSON
								)
		);
	}
	@Test
	void 비인물_타입이_null이면_거부한다() {
		/*
		 * entityType 을 enum 으로 바꾼 뒤로
		 * "DOG" 같은 임의 문자열은 컴파일 단계에서 막힌다.
		 * 런타임에 남는 잘못된 값은 null 뿐이다.
		 */
		assertThrows(
				IllegalArgumentException.class,
				() -> mentionCandidateService.createNonPersonCandidate(
						UUID.randomUUID(),
						"카페",
						"카페",
						null
				)
		);
	}
}