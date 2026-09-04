package com.tada.tada.curator.repository;

import com.tada.tada.curator.entity.MemoryPerson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MemoryPersonRepository extends JpaRepository<MemoryPerson, UUID> {

	List<MemoryPerson> findAllByUserIdAndDisplayNameIn(
			UUID userId,
			Collection<String> displayNames
	);
	
	List<MemoryPerson> findAllByUserId(UUID userId);
}
