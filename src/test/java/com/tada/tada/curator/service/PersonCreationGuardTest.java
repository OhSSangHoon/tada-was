package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.MentionCandidate;
import com.tada.tada.curator.entity.MentionCandidateStatus;
import com.tada.tada.curator.entity.MentionEntityType;
import com.tada.tada.curator.entity.PersonDistinctPair;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class PersonCreationGuardTest {

	private MentionCandidateRepository mentionCandidateRepository;
	private PersonNormalizer personNormalizer;
	private PersonCreationGuard personCreationGuard;

	@BeforeEach
	void setUp() {
		mentionCandidateRepository =
				Mockito.mock(MentionCandidateRepository.class);

		personNormalizer =
				new PersonNormalizer();

		personCreationGuard =
				new PersonCreationGuard(
						mentionCandidateRepository,
						personNormalizer
				);
	}

	@Test
	void 동일_rawText가_한_인물에게만_연결된_이력이_2건_이상이면_재사용한다() {
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		List<MentionCandidate> histories = List.of(
				createPersonCandidate("민혁상", "민혁상", personId),
				createPersonCandidate("민혁상", "민혁상", personId)
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
				Optional.of(personId),
				result
		);
	}

	@Test
	void 동일_rawText_이력이_1건뿐이면_재사용하지_않는다() {
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
				Optional.empty(),
				result
		);
	}

	@Test
	void 동일_rawText가_한_일기_안에서_여러_번_나와도_이력_1회로_센다() {
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();
		UUID diaryId = UUID.randomUUID();

		List<MentionCandidate> histories = List.of(
				MentionCandidate.create(
						diaryId,
						"민혁상",
						"민혁상",
						MentionEntityType.PERSON,
						MentionCandidateStatus.CONFIRMED,
						personId
				),
				MentionCandidate.create(
						diaryId,
						"민혁상",
						"민혁상",
						MentionEntityType.PERSON,
						MentionCandidateStatus.CONFIRMED,
						personId
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
						"민혁에게서",
						"민혁",
						personIdC
				),
				createPersonCandidate(
						"민혁한테서",
						"민혁",
						personIdC
				),
				createPersonCandidate(
						"민혁에게",
						"민혁",
						personIdC
				),
				createPersonCandidate(
						"민혁한테",
						"민혁",
						personIdC
				),
				createPersonCandidate(
						"민혁하고",
						"민혁",
						personIdA
				)
		);

		when(mentionCandidateRepository.findPersonMatchHistory(
				eq(userId),
				eq("민혁과"),
				eq("민혁"),
				eq(MentionCandidateStatus.CONFIRMED)
		)).thenReturn(histories);

		Optional<UUID> result =
				personCreationGuard.findReusablePerson(
						userId,
						"민혁과",
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
						"민혁에게서",
						"민혁",
						personIdC
				),
				createPersonCandidate(
						"민혁한테서",
						"민혁",
						personIdC
				),
				createPersonCandidate(
						"민혁에게",
						"민혁",
						personIdC
				),
				createPersonCandidate(
						"민혁한테",
						"민혁",
						personIdA
				)
		);

		when(mentionCandidateRepository.findPersonMatchHistory(
				eq(userId),
				eq("민혁과"),
				eq("민혁"),
				eq(MentionCandidateStatus.CONFIRMED)
		)).thenReturn(histories);

		Optional<UUID> result =
				personCreationGuard.findReusablePerson(
						userId,
						"민혁과",
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
				createPersonCandidate("민혁에게서", "민혁", personIdC),
				createPersonCandidate("민혁한테서", "민혁", personIdC),
				createPersonCandidate("민혁에게", "민혁", personIdC),

				createPersonCandidate("민혁한테", "민혁", personIdA),
				createPersonCandidate("민혁하고", "민혁", personIdA),
				createPersonCandidate("민혁과", "민혁", personIdA)
		);

		when(mentionCandidateRepository.findPersonMatchHistory(
				eq(userId),
				eq("민혁께"),
				eq("민혁"),
				eq(MentionCandidateStatus.CONFIRMED)
		)).thenReturn(histories);

		Optional<UUID> result =
				personCreationGuard.findReusablePerson(
						userId,
						"민혁께",
						"민혁"
				);

		assertEquals(
				Optional.empty(),
				result
		);
	}

	@Test
	void 애매한_조사로만_도달한_과거_이력은_normalizedText_재사용에서_제외한다() {
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		/*
		 * "김성은", "김성이" 는 애매한 조사(은/이)를 떼야만 "김성" 이 된다.
		 * safeMatchCandidates 에는 없으므로, 과거 이력이 아무리 쌓여도
		 * "김성" 재사용의 근거가 되면 안 된다.
		 */
		List<MentionCandidate> histories = List.of(
				createPersonCandidate("김성은", "김성", personId),
				createPersonCandidate("김성이", "김성", personId)
		);

		when(mentionCandidateRepository.findPersonMatchHistory(
				eq(userId),
				eq("김성"),
				eq("김성"),
				eq(MentionCandidateStatus.CONFIRMED)
		)).thenReturn(histories);

		Optional<UUID> result =
				personCreationGuard.findReusablePerson(
						userId,
						"김성",
						"김성"
				);

		assertEquals(
				Optional.empty(),
				result
		);
	}

	@Test
	void 입력이_애매한_조사를_거쳐야만_도달하면_이력_재사용_자체를_시도하지_않는다() {
		UUID userId = UUID.randomUUID();
		UUID personId = UUID.randomUUID();

		/*
		 * 과거 이력("민수와", "민수를")은 안전 조사로만 도달해 그 자체로는 안정적이다.
		 * 하지만 이번 입력 "민수씨" 는 애매한 조사 씨를 떼야만 "민수" 가 되므로
		 * 이력이 안정적이어도 재사용을 시도조차 하면 안 된다.
		 */
		List<MentionCandidate> histories = List.of(
				createPersonCandidate("민수와", "민수", personId),
				createPersonCandidate("민수를", "민수", personId)
		);

		when(mentionCandidateRepository.findPersonMatchHistory(
				eq(userId),
				eq("민수씨"),
				eq("민수"),
				eq(MentionCandidateStatus.CONFIRMED)
		)).thenReturn(histories);

		Optional<UUID> result =
				personCreationGuard.findReusablePerson(
						userId,
						"민수씨",
						"민수"
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
				MentionEntityType.PERSON,
				MentionCandidateStatus.CONFIRMED,
				matchedPersonId
		);
	}




	@Test
	void distinctPair는_canonical_순서와_self_pair_금지를_강제한다() {
		/*
		 * 실제 DB 에는 쌍 unique 도 self-pair CHECK 도 없다. (명세 7.8)
		 * PersonDistinctPair 는 현재 매칭·CreationGuard 에서 사용하지 않지만,
		 * 엔티티 불변식은 병합·교정 기능에서 쓰이기 전에 확보해 둔다.
		 */
		UUID first = UUID.randomUUID();
		UUID second = UUID.randomUUID();

		PersonDistinctPair forward =
				PersonDistinctPair.create(first, second);

		PersonDistinctPair reverse =
				PersonDistinctPair.create(second, first);

		assertTrue(
				forward.getPersonIdA().toString()
						.compareTo(
								forward.getPersonIdB().toString()
						) < 0
		);

		assertEquals(
				forward.getPersonIdA(),
				reverse.getPersonIdA()
		);

		assertEquals(
				forward.getPersonIdB(),
				reverse.getPersonIdB()
		);

		assertThrows(
				IllegalArgumentException.class,
				() -> PersonDistinctPair.create(first, first)
		);

		assertThrows(
				IllegalArgumentException.class,
				() -> PersonDistinctPair.create(first, null)
		);
	}
}
