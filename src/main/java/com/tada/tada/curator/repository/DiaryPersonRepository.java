package com.tada.tada.curator.repository;

import com.tada.tada.curator.entity.DiaryPerson;
import com.tada.tada.curator.entity.DiaryPersonId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryPersonRepository extends JpaRepository<DiaryPerson, DiaryPersonId> {
}
