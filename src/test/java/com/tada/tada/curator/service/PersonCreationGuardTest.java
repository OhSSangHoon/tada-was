package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.entity.MentionCandidateStatus;
import com.tada.tada.curator.repository.MentionCandidateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class PersonCreationGuardTest {

	private MentionCandidateRepository mentionCandidateRepository;
	private PersonCreationGuard personCreationGuard;

	@BeforeEach
	void setUp() {
		mentionCandidateRepository =
				Mockito.mock(MentionCandidateRepository.class);

		personCreationGuard =
				new PersonCreationGuard(
						mentionCandidateRepository
				);
	}

	@Test
	void 동일_rawText가_한_인물에게만_연결된_이력이_있으면_1회부터_재사용한다() {
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		MentionCandidate history = createPersonCandidate(
				"민혁상",
				"민혁상",
				personId
		);

		when(mentionCandidateRepository.findPersonMatchHistory(
				eq(userId),
				eq("민혁상"),
				eq("민혁상"),
				eq(MentionCandidateStatus.CONFIRMED)
		)).thenReturn(List.of(history));

		Optional<UUID> result =
				personCreationGuard.findReusablePerson(
						userId,
						"민혁상",
						"민혁상"
				);

		assertEquals(
				Optional.of(personId),
				result
		);
	}

	@Test
	void 동일_rawText가_여러_인물에게_연결된_이력이_있으면_재사용하지_않는다() {
		UUID userId = UUID.randomUUID();

		UUID personIdA = UUID.randomUUID();
		UUID personIdC = UUID.randomUUID();

		List<MentionCandidate> histories = List.of(
				createPersonCandidate(
						"민혁상",
						"민혁상",
						personIdC
				),
				createPersonCandidate(
						"민혁상",
						"민혁상",
						personIdA
				)
		);

		when(mentionCandidateRepository.findPersonMatchHistory(
				eq(userId),
				eq("민혁상"),
				eq("민혁상"),
				eq(MentionCandidateStatus.CONFIRMED)
		)).thenReturn(histories);

		Optional<UUID> result =
				personCreationGuard.findReusablePerson(
						userId,
						"민혁상",
						"민혁상"
				);

		assertEquals(
				Optional.empty(),
				result
		);
	}

	@Test
	void normalizedText_이력이_4대1이면_우세한_인물을_재사용한다() {
		UUID userId = UUID.randomUUID();

		UUID personIdA = UUID.randomUUID();
		UUID personIdC = UUID.randomUUID();

		List<MentionCandidate> histories = List.of(
				createPersonCandidate(
						"민혁씨",
						"민혁",
						personIdC
				),
				createPersonCandidate(
						"민혁님",
						"민혁",
						personIdC
				),
				createPersonCandidate(
						"김민혁",
						"민혁",
						personIdC
				),
				createPersonCandidate(
						"민혁이",
						"민혁",
						personIdC
				),
				createPersonCandidate(
						"민혁형",
						"민혁",
						personIdA
				)
		);

		when(mentionCandidateRepository.findPersonMatchHistory(
				eq(userId),
				eq("새로운민혁표현"),
				eq("민혁"),
				eq(MentionCandidateStatus.CONFIRMED)
		)).thenReturn(histories);

		Optional<UUID> result =
				personCreationGuard.findReusablePerson(
						userId,
						"새로운민혁표현",
						"민혁"
				);

		assertEquals(
				Optional.of(personIdC),
				result
		);
	}

	@Test
	void normalizedText_이력이_3대1이면_우세비율이_부족해서_재사용하지_않는다() {
		UUID userId = UUID.randomUUID();

		UUID personIdA = UUID.randomUUID();
		UUID personIdC = UUID.randomUUID();

		List<MentionCandidate> histories = List.of(
				createPersonCandidate(
						"민혁씨",
						"민혁",
						personIdC
				),
				createPersonCandidate(
						"민혁님",
						"민혁",
						personIdC
				),
				createPersonCandidate(
						"김민혁",
						"민혁",
						personIdC
				),
				createPersonCandidate(
						"민혁형",
						"민혁",
						personIdA
				)
		);

		when(mentionCandidateRepository.findPersonMatchHistory(
				eq(userId),
				eq("새로운민혁표현"),
				eq("민혁"),
				eq(MentionCandidateStatus.CONFIRMED)
		)).thenReturn(histories);

		Optional<UUID> result =
				personCreationGuard.findReusablePerson(
						userId,
						"새로운민혁표현",
						"민혁"
				);

		assertEquals(
				Optional.empty(),
				result
		);
	}

	@Test
	void normalizedText_이력이_3대3이면_경쟁하므로_재사용하지_않는다() {
		UUID userId = UUID.randomUUID();

		UUID personIdA = UUID.randomUUID();
		UUID personIdC = UUID.randomUUID();

		List<MentionCandidate> histories = List.of(
				createPersonCandidate("표현C1", "민혁", personIdC),
				createPersonCandidate("표현C2", "민혁", personIdC),
				createPersonCandidate("표현C3", "민혁", personIdC),

				createPersonCandidate("표현A1", "민혁", personIdA),
				createPersonCandidate("표현A2", "민혁", personIdA),
				createPersonCandidate("표현A3", "민혁", personIdA)
		);

		when(mentionCandidateRepository.findPersonMatchHistory(
				eq(userId),
				eq("새로운민혁표현"),
				eq("민혁"),
				eq(MentionCandidateStatus.CONFIRMED)
		)).thenReturn(histories);

		Optional<UUID> result =
				personCreationGuard.findReusablePerson(
						userId,
						"새로운민혁표현",
						"민혁"
				);

		assertEquals(
				Optional.empty(),
				result
		);
	}

	@Test
	void 안정적인_과거_이력이_있어도_block된_인물은_재사용하지_않는다() {
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		MentionCandidate history = createPersonCandidate(
				"민혁상",
				"민혁상",
				personId
		);

		when(mentionCandidateRepository.findPersonMatchHistory(
				eq(userId),
				eq("민혁상"),
				eq("민혁상"),
				eq(MentionCandidateStatus.CONFIRMED)
		)).thenReturn(List.of(history));

		Optional<UUID> result =
				personCreationGuard.findReusablePerson(
						userId,
						"민혁상",
						"민혁상",
						Set.of(personId)
				);

		assertEquals(
				Optional.empty(),
				result
		);
	}
	@Test
	void blockedPerson을_제외한_뒤_남은_유일한_rawText_인물을_재사용한다() {
		UUID userId = UUID.randomUUID();

		UUID blockedPersonId = UUID.randomUUID();
		UUID reusablePersonId = UUID.randomUUID();

		List<MentionCandidate> histories = List.of(
				createPersonCandidate(
						"민수형",
						"민수형",
						blockedPersonId
				),
				createPersonCandidate(
						"민수형",
						"민수형",
						reusablePersonId
				)
		);

		when(mentionCandidateRepository.findPersonMatchHistory(
				eq(userId),
				eq("민수형"),
				eq("민수형"),
				eq(MentionCandidateStatus.CONFIRMED)
		)).thenReturn(histories);

		Optional<UUID> result =
				personCreationGuard.findReusablePerson(
						userId,
						"민수형",
						"민수형",
						Set.of(blockedPersonId)
				);

		assertEquals(
				Optional.of(reusablePersonId),
				result
		);
	}

	private MentionCandidate createPersonCandidate(
			String rawText,
			String normalizedText,
			UUID matchedPersonId
	) {
		return MentionCandidate.create(
				UUID.randomUUID(),
				rawText,
				normalizedText,
				"PERSON",
				MentionCandidateStatus.CONFIRMED,
				matchedPersonId
		);
	}
}