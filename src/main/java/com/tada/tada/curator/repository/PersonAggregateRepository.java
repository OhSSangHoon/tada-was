package com.tada.tada.curator.repository;

import com.tada.tada.curator.entity.PersonAggregate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PersonAggregateRepository extends JpaRepository<PersonAggregate, UUID> {
}
