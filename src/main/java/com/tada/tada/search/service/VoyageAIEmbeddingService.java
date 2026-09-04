package com.tada.tada.search.service;

/*
	VoyageAIEmbeddingService - 텍스트를 벡터로 변환하는 서비스
	
	현재 더미 버전
 */

import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class VoyageAIEmbeddingService {
	
	// Voyage AI voyage-4 모델의 임베딩 차원 수 - 고정값
	private static final int EMBEDDING_DIMENSION = 1024;
	
	public float[] embed(String text) {
		float[] dummyVector = new float[EMBEDDING_DIMENSION];
		
		Random random = new Random(text.hashCode());
		
		for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
			dummyVector[i] = (random.nextFloat() * 2) - 1;
		}
		return dummyVector;
	}
}
