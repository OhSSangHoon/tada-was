package com.tada.tada.global.client;

import com.tada.tada.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class StickerWebhookClientTest {

	private static final String WEBHOOK_URL = "https://n8n.example.com/webhook/sticker-generation";

	private MockRestServiceServer mockServer;
	private StickerWebhookClient stickerWebhookClient;

	@BeforeEach
	void setUp() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
		stickerWebhookClient = new StickerWebhookClient(restClientBuilder, WEBHOOK_URL);
	}

	@Test
	void keyword로_웹훅을_호출해서_이미지_바이트를_반환한다() {
		byte[] imageBytes = {1, 2, 3};

		mockServer.expect(requestTo(WEBHOOK_URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json("{\"keyword\":\"카페\"}"))
				.andRespond(withSuccess(imageBytes, MediaType.IMAGE_JPEG));

		byte[] result = stickerWebhookClient.requestStickerImage("카페");

		assertArrayEquals(imageBytes, result);
		mockServer.verify();
	}

	@Test
	void 웹훅_호출_실패시_502_CustomException으로_변환한다() {
		mockServer.expect(requestTo(WEBHOOK_URL))
				.andRespond(withServerError());

		CustomException exception = assertThrows(
				CustomException.class,
				() -> stickerWebhookClient.requestStickerImage("카페")
		);
		assertEquals(502, exception.getStatusCode());
	}

	@Test
	void 응답_바이트가_비어있으면_502_CustomException을_던진다() {
		mockServer.expect(requestTo(WEBHOOK_URL))
				.andRespond(withSuccess(new byte[0], MediaType.IMAGE_JPEG));

		CustomException exception = assertThrows(
				CustomException.class,
				() -> stickerWebhookClient.requestStickerImage("카페")
		);
		assertEquals(502, exception.getStatusCode());
	}
}
