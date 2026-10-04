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

import com.tada.tada.search.dto.DiaryDistanceDebugProjection;

import java.util.List;
import java.util.UUID;

@Repository
public interface SearchRepository extends JpaRepository<Diary, UUID> {
	
	/*
	   임베딩 기반 검색 결과를 최신순(entry_date DESC)으로 정렬해서 페이지네이션과 함께 조회
	   - threshold: 코사인 거리 기준값보다 작은(=쿼리와 관련 있는) 일기만 후보로 걸러낸 뒤 날짜순 정렬
	   - 유사도순 정렬은 findSimilarDiariesOrderByRelevance 참고

	   @param userId 검색을 요청한 로그인 사용자 ID
	   @param embedding 검색할 쿼리 벡터 - 1024차원 Voyage AI 임베딩
	   @param threshold 유사도(코사인 거리) 임계값
	   @param pageable 페이지 정보
	   @return 최신순으로 정렬된 일기 Page 객체
	 */
	@Query(value = """
               SELECT d.id AS id, d.entry_date AS entryDate, d.title AS title,
                     d.weather AS weather, d.content AS content, d.created_at AS createdAt,
                     s.image_url AS stickerImageUrl
               FROM diaries d
               LEFT JOIN stickers s ON s.diary_id = d.id
               WHERE d.user_id = :userId AND d.status = 'ACTIVE' AND d.embedding IS NOT NULL
                 AND (d.embedding <=> CAST(:embedding AS vector)) < :threshold
               ORDER BY d.entry_date DESC, d.id ASC
            """,
			countQuery = """
                  SELECT COUNT(d.id) FROM diaries d
                  WHERE d.user_id = :userId AND d.status = 'ACTIVE' AND d.embedding IS NOT NULL
                    AND (d.embedding <=> CAST(:embedding AS vector)) < :threshold
                  """,
			nativeQuery = true)
	Page<SearchResultProjection> findSimilarDiariesOrderByEntryDateDesc(
			@Param("userId") UUID userId,
			@Param("embedding") String embedding,
			@Param("threshold") double threshold,
			Pageable pageable
	);
	
	/*
	   임베딩 기반 검색 결과를 오래된순(entry_date ASC)으로 정렬해서 페이지네이션과 함께 조회
	   - threshold: 코사인 거리 기준값보다 작은(=쿼리와 관련 있는) 일기만 후보로 걸러낸 뒤 날짜순 정렬

	   @param userId 검색을 요청한 로그인 사용자 ID
	   @param embedding 검색할 쿼리 벡터 - 1024차원 Voyage AI 임베딩
	   @param threshold 유사도(코사인 거리) 임계값
	   @param pageable 페이지 정보
	   @return 오래된순으로 정렬된 일기 Page 객체
	 */
	@Query(value = """
               SELECT d.id AS id, d.entry_date AS entryDate, d.title AS title,
                     d.weather AS weather, d.content AS content, d.created_at AS createdAt,
                     s.image_url AS stickerImageUrl
               FROM diaries d
               LEFT JOIN stickers s ON s.diary_id = d.id
               WHERE d.user_id = :userId AND d.status = 'ACTIVE' AND d.embedding IS NOT NULL
                 AND (d.embedding <=> CAST(:embedding AS vector)) < :threshold
               ORDER BY d.entry_date ASC, d.id ASC
            """,
			countQuery = """
                  SELECT COUNT(d.id) FROM diaries d
                  WHERE d.user_id = :userId AND d.status = 'ACTIVE' AND d.embedding IS NOT NULL
                    AND (d.embedding <=> CAST(:embedding AS vector)) < :threshold
                  """,
			nativeQuery = true)
	Page<SearchResultProjection> findSimilarDiariesOrderByEntryDateAsc(
			@Param("userId") UUID userId,
			@Param("embedding") String embedding,
			@Param("threshold") double threshold,
			Pageable pageable
	);
	
	/*
	   임베딩 기반 검색 결과를 유사도순(코사인 거리 오름차순)으로 정렬해서 페이지네이션과 함께 조회
	   - threshold: 코사인 거리 기준값보다 작은 일기만 후보로 걸러낸 뒤 쿼리와 가까운 순으로 정렬
	   - 거리가 같으면 id로 정렬해 페이지 간 순서를 고정

	   @param userId 검색을 요청한 로그인 사용자 ID
	   @param embedding 검색할 쿼리 벡터 - 1024차원 Voyage AI 임베딩
	   @param threshold 유사도(코사인 거리) 임계값
	   @param pageable 페이지 정보
	   @return 유사도순으로 정렬된 일기 Page 객체
	 */
	@Query(value = """
               SELECT d.id AS id, d.entry_date AS entryDate, d.title AS title,
                     d.weather AS weather, d.content AS content, d.created_at AS createdAt,
                     s.image_url AS stickerImageUrl
               FROM diaries d
               LEFT JOIN stickers s ON s.diary_id = d.id
               WHERE d.user_id = :userId AND d.status = 'ACTIVE' AND d.embedding IS NOT NULL
                 AND (d.embedding <=> CAST(:embedding AS vector)) < :threshold
               ORDER BY (d.embedding <=> CAST(:embedding AS vector)) ASC, d.id ASC
            """,
			countQuery = """
                  SELECT COUNT(d.id) FROM diaries d
                  WHERE d.user_id = :userId AND d.status = 'ACTIVE' AND d.embedding IS NOT NULL
                    AND (d.embedding <=> CAST(:embedding AS vector)) < :threshold
                  """,
			nativeQuery = true)
	Page<SearchResultProjection> findSimilarDiariesOrderByRelevance(
			@Param("userId") UUID userId,
			@Param("embedding") String embedding,
			@Param("threshold") double threshold,
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
	
	/*
	   [디버그 전용] threshold 필터링 없이, 특정 쿼리 임베딩과 사용자의 전체 일기 사이의
	   코사인 거리를 가까운 순으로 전체 조회
	   - "특정 검색어가 왜 이 일기를 못 찾았는지/엉뚱한 일기를 찾았는지" 진단용
	   - 진단 끝나면 이 메서드는 삭제할 것 (운영 코드 아님)

	   @param userId 사용자 ID
	   @param embedding 쿼리 임베딩 벡터
	   @return 거리순(가까운 순) 정렬된 전체 일기의 title/distance
	 */
	@Query(value = """
	           SELECT d.title AS title,
	                 (d.embedding <=> CAST(:embedding AS vector)) AS distance
	           FROM diaries d
	           WHERE d.user_id = :userId AND d.status = 'ACTIVE' AND d.embedding IS NOT NULL
	           ORDER BY distance ASC
	        """,
			nativeQuery = true)
	List<DiaryDistanceDebugProjection> debugFindAllOrderByDistance(
			@Param("userId") UUID userId,
			@Param("embedding") String embedding
	);
}