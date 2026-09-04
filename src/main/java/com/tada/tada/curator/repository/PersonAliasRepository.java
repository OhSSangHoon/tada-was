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
}
