package com.tada.tada.curator.repository;

import com.tada.tada.curator.entity.MemoryPerson;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemoryPersonRepository extends JpaRepository<MemoryPerson, UUID> {

	List<MemoryPerson> findAllByUserIdAndDisplayNameIn(
			UUID userId,
			Collection<String> displayNames
	);

	List<MemoryPerson> findAllByUserId(UUID userId);

	/*
	 * PersonAggregate 재계산의 직렬화 지점. (명세 17.3)
	 * aggregate row 는 없을 수 있어 잠글 수 없으므로 memory_person 을 mutex 로 쓴다.
	 * 호출부는 personId 를 정렬해 한 건씩 잠근다. (deadlock 방지)
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT person FROM MemoryPerson person WHERE person.id = :personId")
	Optional<MemoryPerson> findByIdForUpdate(
			@Param("personId") UUID personId
	);

	/*
	 * 소유권을 조회 조건에 넣는다.
	 * 없는 사람과 남의 사람이 모두 empty 로 와야 호출부가 둘을 구분하지 않고 404 를 준다.
	 * 403 을 주면 그 id 가 존재한다는 사실이 새어 나간다.
	 */
	Optional<MemoryPerson> findByIdAndUserId(
			UUID id,
			UUID userId
	);

	/*
	 * 홈 사람 목록.
	 *
	 * mentionCount / lastMentionedAt 은 PersonAggregate 캐시를 그대로 읽는다.
	 * ACTIVE 원본 재계산으로 유지되는 값이라 여기서 다시 집계하지 않는다.
	 * mentionCount > 0 이 "ACTIVE 일기에 한 번이라도 나온 사람" 조건이다.
	 *
	 * 정렬 tie-break 를 끝까지 둔다. lastMentionedAt 만으로 끊으면
	 * 같은 날짜의 두 사람 순서가 DB 반환 순서에 의존해 새로고침마다 바뀐다.
	 */
	@Query("""
			SELECT
				person.id AS personId,
				person.displayName AS displayName,
				aggregate.mentionCount AS mentionCount,
				aggregate.lastMentionedAt AS lastMentionedAt
			FROM MemoryPerson person, PersonAggregate aggregate
			WHERE aggregate.personId = person.id
			  AND person.userId = :userId
			  AND aggregate.mentionCount > 0
			ORDER BY aggregate.lastMentionedAt DESC, person.displayName ASC, person.id ASC
			""")
	List<PersonListRow> findPersonList(
			@Param("userId") UUID userId
	);

	interface PersonListRow {

		UUID getPersonId();

		String getDisplayName();

		int getMentionCount();

		LocalDateTime getLastMentionedAt();
	}
}
