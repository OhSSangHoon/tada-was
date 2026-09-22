package com.tada.tada.curator.service;

import com.tada.tada.curator.entity.DiaryPerson;
import com.tada.tada.curator.repository.DiaryPersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/*
 * Diary ACTIVE/TRASHED 전환 시 연결된 인물 집계를 재계산한다. Trash/Restore가 같은 코드다 —
 * 증감이 아니라 ACTIVE 원본 기준 재계산이라 방향과 무관하게 몇 번을 돌려도 같은 값이 나온다.
 * Candidate/Relation/DiaryPerson은 건드리지 않는다 (Trash는 상태 전이일 뿐, 명세 16.1, 16.2).
 * MANDATORY다 — 상위 트랜잭션 밖에서 발행되면 커밋 전 상태 변경을 못 보고 재계산해 값이 틀어진다.
 */
@Service
@RequiredArgsConstructor
public class DiaryStatusChangeService {

	private final DiaryPersonRepository diaryPersonRepository;
	private final PersonAggregateService personAggregateService;

	@Transactional(
			propagation = Propagation.MANDATORY
	)
	public void recalculateAffectedPersons(
			UUID diaryId,
			UUID userId
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

		List<DiaryPerson> diaryPersons =
				diaryPersonRepository.findAllByDiaryId(
						diaryId
				);

		Set<UUID> affectedPersonIds =
				new HashSet<>();

		for (DiaryPerson diaryPerson : diaryPersons) {
			affectedPersonIds.add(
					diaryPerson.getPersonId()
			);
		}

		if (affectedPersonIds.isEmpty()) {
			return;
		}

		personAggregateService.recalculate(
				userId,
				affectedPersonIds
		);
	}
}
