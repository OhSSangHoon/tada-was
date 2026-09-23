package com.tada.tada.search.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tada.tada.global.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Service
public class VoyageAIEmbeddingService {
	
	private static final String VOYAGE_API_URL = "https://api.voyageai.com/v1/embeddings";
	private static final String MODEL = "voyage-4";
	private static final int EMBEDDING_DIMENSION = 1024;
	
	private final RestClient restClient;
	
	public VoyageAIEmbeddingService(@Value("${voyage.api.key}") String apikey) {
		this.restClient = RestClient.builder()
				.baseUrl(VOYAGE_API_URL)
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apikey)
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}
	
	public float[] embed(String text) {
		VoyageEmbeddingRequest request = new VoyageEmbeddingRequest(
				List.of(text), MODEL, null, EMBEDDING_DIMENSION
		);
		
		VoyageEmbeddingResponse response;
		try {
			response = restClient.post()
					.body(request)
					.retrieve()
					.body(VoyageEmbeddingResponse.class);
		} catch (RestClientResponseException e) {
			System.out.println("[Voyage] HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
			if (e.getStatusCode().value() == 429) {
				throw new CustomException("Voyage AI 요청 한도 초과", 429);
			}
			throw new CustomException("Voyage AI 임베딩 생성에 실패했습니다.", 502);
		} catch (Exception e) {
			System.out.println("[Voyage] 예외: " + e.getClass().getName() + " - " + e.getMessage());
			throw new CustomException("Voyage AI 임베딩 생성에 실패했습니다.", 502);
		}
		if (response == null || response.data() == null || response.data().isEmpty()) {
			throw new CustomException("Voyage AI 임베딩 응답이 비어있습니다.", 502);
		}
		
		List<Double> vector = response.data().get(0).embedding();
		if (vector == null) {
			throw new CustomException("Voyage AI 임베딩 응답의 embedding 필드가 비어있습니다.", 502);
		}
		float[] result = new float[vector.size()];
		for (int i = 0; i < vector.size(); i++) {
			result[i] = vector.get(i).floatValue();
		}
		return result;
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	private record VoyageEmbeddingRequest(
			List<String> input,
			String model,
			String input_type,
			int output_dimension
	) {}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	private record VoyageEmbeddingResponse(
			List<EmbeddingData> data,
			String model
	) {}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	private record EmbeddingData(
			List<Double> embedding,
			Integer index
	) {}
}