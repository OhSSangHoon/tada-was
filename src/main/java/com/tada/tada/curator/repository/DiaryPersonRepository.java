package com.tada.tada.curator.repository;

import com.tada.tada.curator.entity.DiaryPerson;
import com.tada.tada.curator.entity.DiaryPersonId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiaryPersonRepository
		extends JpaRepository<DiaryPerson, DiaryPersonId> {

	List<DiaryPerson> findAllByDiaryId(
			UUID diaryId
	);
}