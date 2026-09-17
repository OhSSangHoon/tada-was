package com.tada.tada.diary.repository;

import com.tada.tada.diary.entity.DiaryStatus;
import com.tada.tada.diary.entity.Sticker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
		사용자가 모은 스티커를 페이지네이션과 함께 조회 (코드리뷰 반영 - 페이지네이션 전환)
       
       - Sticker에는 userId가 없고 diaryId만 있어서, Diary와 조인해서 소유자 확인
          (Sticker-Diary 사이에 연관관계 매핑을 안했으므로 HQL 세미조인(theta-join) 형태로 처리)
       - soft delete 고려 - status가 ACTIVE인 일기에 달린 스티커만 노출 (휴지통 스티커는 제외)
       - 매직 문자열('ACTIVE') 대신 DiaryStatus enum 파라미터로 비교 (코드리뷰 반영)
       - 정렬 방향은 Pageable에 포함된 Sort로 동적 처리 (최신순/오래된순)
       
       @param userId 로그인한 사용자 ID
       @param status 조회 대상 일기 상태 (DiaryStatus.ACTIVE 고정 전달)
       @param pageable 페이지 정보 + 정렬 정보
       @return 사용자가 모은 스티커 Page 객체
	 */
	
	@Query("""
		SELECT s FROM Sticker s, Diary d
		WHERE s.diaryId = d.id
		AND d.userId = :userId
		AND d.status = :status
	""")
	Page<Sticker> findByUserId(
			@Param("userId")UUID userId,
			@Param("status")DiaryStatus status,
			Pageable pageable
			);
}
