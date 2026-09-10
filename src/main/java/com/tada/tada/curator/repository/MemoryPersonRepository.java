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
	 * PersonAggregate 재계산의 직렬화 지점 (명세 17.3). aggregate row가 없을 수 있어
	 * memory_person을 mutex로 쓰며, 호출부는 personId 정렬 후 한 건씩 잠근다 (deadlock 방지).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT person FROM MemoryPerson person WHERE person.id = :personId")
	Optional<MemoryPerson> findByIdForUpdate(
			@Param("personId") UUID personId
	);

	Optional<MemoryPerson> findByIdAndUserId(
			UUID id,
			UUID userId
	);

	/*
	 * 홈 목록. mentionCount/lastMentionedAt은 PersonAggregate 캐시를 그대로 읽는다
	 * (mentionCount>0 이 "ACTIVE 일기에 언급된 적 있음" 조건).
	 * tie-break를 끝까지 둔다 — lastMentionedAt만으로 끊으면 같은 날짜 순서가 DB 반환 순서에 따라 흔들린다.
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
