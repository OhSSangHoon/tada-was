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

	/*
	 * Diary 영구삭제 때 Diary 도메인이 CuratorCleanupService 를 통해 호출한다.
	 *
	 * MemoryPerson 은 여러 Diary 가 공유하므로 여기서 지우지 않는다.
	 * 이 연결 행만 제거한다.
	 */
	void deleteByDiaryId(
			UUID diaryId
	);
}