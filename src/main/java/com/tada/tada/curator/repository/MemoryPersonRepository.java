package com.tada.tada.curator.repository;

import com.tada.tada.curator.entity.MemoryPerson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MemoryPersonRepository extends JpaRepository<MemoryPerson, UUID> {
}
