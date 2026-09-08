package com.tada.tada.diary.repository;

import com.tada.tada.diary.entity.Sticker;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StickerRepository extends JpaRepository<Sticker, UUID> {
	
	List<Sticker> findByDiaryIdIn(List<UUID> diaryIds);

	/*
		사용자가 모은 스티커 전체 목록 조회 (페이지네이션 없음 - 프론트에서 무한 스크롤)
		
		- Sticker에는 `userId가 없고 diaryId만 있어서, Diary와 조인해서 소유자 확인
			(Sticker-Diary 사이에 연관관계 매핑을 안했으므로 HQL 세미조인(theta-join) 형태로 처리)
		- soft delete 고려 - d.status = 'ACTIVE'인 일기에 달린 스티커만 노출 (휴지통 스티커는 제외)
		- 정렬 방향 고정없이 Sort 파라미터로 동적 처리 (최신순/오래된순)
		
		@param userId 로그인한 사용자 ID
		@parma sort createdAt 기준 정렬 방향
		@return 사용자가 모은 스티커 전체 리스트
	 */
	
	@Query("""
		SELECT s FROM Sticker s, Diary d
		WHERE s.diaryId = d.id
		AND d.userId = :userId
		AND d.status = 'ACTIVE'
	""")
	List<Sticker> findByUserId(@Param("userId") UUID userId, Sort sort);
}
