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
 * Diary 의 ACTIVE / TRASHED 상태가 바뀌었을 때
 * 그 일기에 연결된 인물들의 집계를 다시 계산한다.
 *
 * Trash 와 Restore 가 같은 코드다.
 * PersonAggregate 는 증감이 아니라 ACTIVE 일기 원본에서 재계산하므로,
 * 상태가 어느 방향으로 바뀌었는지 알 필요가 없다.
 * 재계산은 몇 번을 돌려도 같은 값이 나온다.
 *
 * Candidate, Relation, DiaryPerson 은 건드리지 않는다.
 * Trash 는 물리삭제가 아니라 상태 전이이고,
 * Restore 때 그대로 재사용해야 한다. (명세 16.1, 16.2)
 *
 * MANDATORY 다. 상위 트랜잭션이 없으면 즉시 예외가 난다.
 * 트랜잭션 밖에서 이벤트가 발행되면 아직 commit 되지 않은 상태 변경을
 * 못 본 채로 집계해서, 바뀌기 전 값이 그대로 저장된다.
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

		/*
		 * 인물이 없는 일기는 재계산할 대상도 없다.
		 * recalculate 는 빈 집합이면 그냥 반환하지만
		 * 불필요한 소유권 조회를 피하려고 여기서 끊는다.
		 */
		if (affectedPersonIds.isEmpty()) {
			return;
		}

		personAggregateService.recalculate(
				userId,
				affectedPersonIds
		);
	}
}
