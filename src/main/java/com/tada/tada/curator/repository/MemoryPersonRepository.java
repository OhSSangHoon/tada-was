package com.tada.tada.curator.repository;

import com.tada.tada.curator.entity.MemoryPerson;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
