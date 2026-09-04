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
		임베딩 벡터를 기반으로 유사한 일기 검색 메서드
		
		쿼리 설명
		- CAST(d.embedding AS float4[]) : embedding 필드를 float4[] 배열로 변환
			(pgvector 연산을 위해 명시적 타입 변환 필요)
		
		- d.embedding <-> CAST(:embedding AS vector) :
			pgvector의 코사인 거리 연산자 "<->" 를 사용
			쿼리 벡터와 저장된 embedding 사이의 거리를 계산
			거리가 작을수록 유사도 높음 (0에 가까울수록 높음)
		
		- WHERE d.status = 'ACTIVE' :
			소프트 삭제된 일기(status='TRASHED')는 제외
			"ACTIVE" 상태인 일기만 검색 결과에 포함
			
		- ORDER BY d.embedding <-> CAST(:embedding AS vector) ASC :
			검색 결과를 거리순으로 정렬 (오름차순)
			가장 유사한 일기가 먼저 반환
			
		@param embedding 검색할 쿼리 벡터 (길이:1024 차원, Voyage AI 임베딩)
		@param limit 반환할 최대 일기 개수
		@return 유사도순 일기 리스트
		
		예시 자연어 입력 - 1024차원 벡터로 임베딩 - 가장 유사한 일기 찾음 - RAG 모델 검색결과로 응답 생성
	 */
	@Query(value = """
       SELECT d.id AS id, d.entry_date AS entryDate, d.title AS title,
             d.weather AS weather, d.content AS content, d.created_at AS createdAt
       FROM diaries d
       WHERE d.status = 'ACTIVE' AND d.embedding IS NOT NULL
       ORDER BY d.embedding <-> CAST(:embedding AS vector) ASC
       LIMIT :limit
    """, nativeQuery = true)
	List<SearchResultProjection> findSimilarDiaries(
			@Param("embedding") String embedding,
			@Param("limit") int limit
	);
	
	/*
		임베딩 벡터를 기반으로 유사한 일기를 페이지네이션과 함께 검색
		
		쿼리설명
		- findSimilarDiaries와 동일하지만 페이지네이션 지원
		- Pageable 객체의 size(기본 3)로 조회 개수 결정
		- Pageable 객체의 offset으로 시작 위치 결정
		
		@param embedding 검색할 쿼리 벡터 - 1024차원 Voyage AI 임베딩
		@param pageable 페이지 정보 -Pageable.offset(3)
		@return 유사도순 일기 Page 객체
	 */
	@Query(value = """
       SELECT d.id AS id, d.entry_date AS entryDate, d.title AS title,
             d.weather AS weather, d.content AS content, d.created_at AS createdAt
       FROM diaries d
       WHERE d.status = 'ACTIVE' AND d.embedding IS NOT NULL
       ORDER BY d.embedding <-> CAST(:embedding AS vector) ASC
    """,
			countQuery = """
                SELECT COUNT(d.id) FROM diaries d
                WHERE d.status = 'ACTIVE' AND d.embedding IS NOT NULL
                """,
			nativeQuery = true)
	Page<SearchResultProjection> findSimilarDiariesWithPagination(
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

