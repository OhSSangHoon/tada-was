package com.tada.tada.curator.repository;

import com.tada.tada.curator.entity.PersonDistinctPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PersonDistinctPairRepository
		extends JpaRepository<PersonDistinctPair, UUID> {

	@Query("""
			SELECT CASE
				WHEN COUNT(pair) > 0 THEN true
				ELSE false
			END
			FROM PersonDistinctPair pair
			WHERE (
				pair.personIdA = :personIdA
				AND pair.personIdB = :personIdB
			)
			OR (
				pair.personIdA = :personIdB
				AND pair.personIdB = :personIdA
			)
			""")
	boolean existsDistinctPair(
			@Param("personIdA") UUID personIdA,
			@Param("personIdB") UUID personIdB
	);

	@Query("""
			SELECT pair
			FROM PersonDistinctPair pair
			WHERE pair.personIdA IN :personIds
			   OR pair.personIdB IN :personIds
			""")
	List<PersonDistinctPair> findAllByPersonIds(
			@Param("personIds") Collection<UUID> personIds
	);
}