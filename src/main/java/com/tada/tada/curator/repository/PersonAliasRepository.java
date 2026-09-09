package com.tada.tada.curator.repository;

import com.tada.tada.curator.entity.PersonAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PersonAliasRepository extends JpaRepository<PersonAlias, UUID> {
	
	List<PersonAlias> findAllByOwnerUserIdAndNormalizedTextIn(
			UUID ownerUserId,
			Collection<String> normalizedTexts
	);
	
	List<PersonAlias> findAllByOwnerUserId(UUID ownerUserId);

	/*
	 * 사람 목록의 aliases 를 한 번에 채운다.
	 * 사람마다 조회하면 N+1 이라 personId 를 모아 한 번에 읽는다.
	 */
	List<PersonAlias> findAllByOwnerUserIdAndPersonIdIn(
			UUID ownerUserId,
			Collection<UUID> personIds
	);
}
