package com.tada.tada.global.client;

import com.tada.tada.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DiaryAnalysisClientTest {

	private static final String WEBHOOK_URL = "https://n8n.example.com/webhook/diary-analysis";

	private MockRestServiceServer mockServer;
	private DiaryAnalysisClient diaryAnalysisClient;

	@BeforeEach
	void setUp() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
		diaryAnalysisClient = new DiaryAnalysisClient(restClientBuilder, WEBHOOK_URL);
	}

	@Test
	void 본문과_날씨로_웹훅을_호출해서_추출결과를_반환한다() {
		mockServer.expect(requestTo(WEBHOOK_URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json("{\"content\":\"오늘은 카페에 갔다\",\"weather\":\"맑음\"}"))
				.andRespond(withSuccess("""
						{
						  "title": "여유로운 오후",
						  "compressedKeyword": "여유",
						  "extractedKeywords": ["카페", "여유"],
						  "persons": [{"ref": "p1", "rawText": "지수", "entityType": "PERSON"}],
						  "places": [],
						  "activities": []
						}
						""", MediaType.APPLICATION_JSON));

		DiaryAnalysisResponse response = diaryAnalysisClient.analyze("오늘은 카페에 갔다", "맑음");

		assertEquals("지수", response.toExtractionResult().persons().get(0).rawText());
		mockServer.verify();
	}

	@Test
	void 웹훅_호출_실패시_502_CustomException으로_변환한다() {
		mockServer.expect(requestTo(WEBHOOK_URL))
				.andRespond(withServerError());

		CustomException exception = assertThrows(
				CustomException.class,
				() -> diaryAnalysisClient.analyze("내용", null)
		);
		assertEquals(502, exception.getStatusCode());
	}

	@Test
	void 응답_본문이_비어있으면_502_CustomException을_던진다() {
		mockServer.expect(requestTo(WEBHOOK_URL))
				.andRespond(withSuccess("", MediaType.APPLICATION_JSON));

		CustomException exception = assertThrows(
				CustomException.class,
				() -> diaryAnalysisClient.analyze("내용", null)
		);
		assertEquals(502, exception.getStatusCode());
	}
}
