package com.tada.tada.domain.diary.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "diaries")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Diary {
	
	// --------------- Primary Key
	@Id		//id를 PK로 지정
	@Column(name = "id", columnDefinition = "uuid")		// PostgresSQL의 UUID타입
	private UUID id;	// 일기 고유 ID
	
	// --------------- Foreign Key
	// 작성한 사용자의 ID
	// nullable = false: NOY NULL 제약
	// columnDefinition = "uuid" : PostgreSQL UUID 타입
	@Column(name = "user_id", nullable = false, columnDefinition = "uuid")
	private UUID userId;	// USERS 테이블의 user_id와 매핑
	
	// --------------- 일기 기본 정보
	// 날짜 선택 (과거의 날짜 선택 가능)
	// LocalDate : 년/월/일만 저장
	@Column(name = "entry_date", nullable = false)
	private LocalDate entryDate;	// 일기 날짜
	
	// 일기제목-필수
	@Column(name = "title", nullable = false)
	private String title;	// 일기 제목
	
	// 날씨정보-선택
	@Column(name = "weather")
	private String weather;	// 날씨정보
	
	// 일기 본문 내용
	// columnDefinition = "test": varchar보다 큰 용량
	@Column(name = "content", columnDefinition = "text")
	private String content;	// 일기본문
	
	// --------------- AI 임베딩 (RAG 검색용)
	// columnDefinition = "vector(1024)" : pgvector타입, 1024차원
	// float[]: Java에서 벡터를 float 배열로 표현
	// 일기 내용을 Voyage AI가 압축한 벡터 - 유사도 검사
	@Column(name = "embedding", columnDefinition = "vector(1024)")
	private float[] embedding;	// 1024차원 Voyage AI 임베딩 벡터
	
	// --------------- Soft Delete (논리적 삭제)
	// DB삭제가 아닌 상태만 변경
	// nullable = false : 필수
	@Column(name = "status", nullable = false, length = 20)
	private String status;	// "ACTIVE"(정상) 또는 "TRASHED"(삭제)
	
	// 삭제된 시간 -soft delete용
	// status가 "TRASHED"만 값 존재
	// nullable : null 허용
	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;	// 삭제시간
	
	// --------------- 생성 시간 (자동 관리)
	// CreatedDate : 자동으로 현재시간 입력
	// updatable = false : 생성되면 수정 불가
	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;	// 일기 생성시간 - 자동
	
	
	// --------------- Soft Delete 관련 메서드
	
	/*
		일기를 삭제 할 때 호출
		DB에서 삭제 하지 않고 , 상태와 시간만 변경
	 */
	public void trash() {
		this.status = "TRASHED";	// 상태를 삭제 로 변경
		this.deletedAt = LocalDateTime.now();	// 현재 시간을 삭제 시간으로 저장
	}
	
	/*
		삭제된 일기를 복구할 때 호출
		"TRASHED" 에서 "ACTIVE"로 상태 변경
	 */
	public void restore() {
		this.status = "ACTIVE";		// 상태를 정상 으로 변경
		this.deletedAt = null;		// 삭제 시간 제거
	}
	
	/*
		일기가 "ACTIVE" 상태인지 확인
		return true면 "ACTIVE", false면 "TRASHED"
	 */
	public boolean isActive() {
		return "ACTIVE".equals(this.status);
	}
}
