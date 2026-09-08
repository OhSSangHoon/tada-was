package com.tada.tada.search.repository;

import com.tada.tada.diary.entity.DiaryStatus;
import com.tada.tada.diary.entity.Diary;
import com.tada.tada.search.dto.SearchResultProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface SearchRepository extends JpaRepository<Diary, UUID> {
	
	/*
	   임베딩 벡터를 기반으로 유사한 일기를 페이지네이션과 함께 검색
	   
	   쿼리설명
	   - WHERE d.user_id = :userId 로 로그인한 본인의 일기만 검색 대상으로 제한
	   - Pageable 객체의 size(기본 3)로 조회 개수 결정
	   - Pageable 객체의 offset으로 시작 위치 결정
	   
	   @param userId 검색을 요청한 로그인 사용자 ID
	   @param embedding 검색할 쿼리 벡터 - 1024차원 Voyage AI 임베딩
	   @param pageable 페이지 정보 -Pageable.offset(3)
	   @return 유사도순 일기 Page 객체
	 */
	@Query(value = """
       SELECT d.id AS id, d.entry_date AS entryDate, d.title AS title,
             d.weather AS weather, d.content AS content, d.created_at AS createdAt
       FROM diaries d
       WHERE d.user_id = :userId AND d.status = 'ACTIVE' AND d.embedding IS NOT NULL
       ORDER BY d.embedding <-> CAST(:embedding AS vector) ASC
    """,
			countQuery = """
                SELECT COUNT(d.id) FROM diaries d
                WHERE d.user_id = :userId AND d.status = 'ACTIVE' AND d.embedding IS NOT NULL
                """,
			nativeQuery = true)
	Page<SearchResultProjection> findSimilarDiariesWithPagination(
			@Param("userId") UUID userId,
			@Param("embedding") String embedding,
			Pageable pageable
	);
	
	/*
	   사용자 ID와 정상 상태 기준으로 일기 조회
	   - 데이터 검증, 테스트, 관리 목적으로 사용
	   
	   @param userId 검색할 사용자 ID
	   @param status 일기 상태 ("ACTIVE" or "TRASHED")
	   @return 해당하는 일기 리스트
	 */
	List<Diary> findByUserIdAndStatus(UUID userId, DiaryStatus status);
	
	/*
	   사용자 ID 기준으로 정상 일기 개수 조회
	   - 사용자가 작성한 총 일기 수 확인
	   
	   @param userId 사용자 ID
	   @param status 일기 상태 ("ACTIVE" OR "TRASHED")
	   @return 해당하는 일기 개수
	 */
	long countByUserIdAndStatus(UUID userId, DiaryStatus status);
	
	/*
	   임베딩 벡터를 특정 일기(diaryId)에 저장/갱신
	   
	   -DiaryEmbeddingEventListener에서 사용
	   -Entity를 조회해서 save() 하는대신, 네이티브 UPDATE 쿼리로 직접 갱신
		  (Diary.embedding이 아직 float[]로 매핑되어 있어 save()시 타입문제 우려 -> 우회
	   -@Modifying: SELECT가 아니라 UPDATE 쿼리 표시
	   -@Transactional : @Modifying 쿼리는 트랜잭션 안에서 실행
	   
	   @param diaryId 갱신할 일기 ID
	   @param embedding 새로 계산된 임베딩 벡터 문자열
	 */
	@Modifying
	@Transactional
	@Query(value = "UPDATE diaries SET embedding = CAST(:embedding AS vector) WHERE id = :diaryId", nativeQuery = true)
	void updateEmbedding(@Param("diaryId") UUID diaryId, @Param("embedding") String embedding);
	
	
	@Query(value = "SELECT d.id FROM diaries d WHERE d.embedding IS NULL", nativeQuery = true)
	List<UUID> findDiaryIdsWithoutEmbedding();
}