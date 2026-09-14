package com.tada.tada.global.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SupabaseStorageClientTest {

	private static final String STORAGE_URL = "https://example.supabase.co/storage/v1";
	private static final String SERVICE_ROLE_KEY = "test-service-role-key";
	private static final String BUCKET = "stickers";

	private MockRestServiceServer mockServer;
	private SupabaseStorageClient supabaseStorageClient;

	@BeforeEach
	void setUp() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
		supabaseStorageClient = new SupabaseStorageClient(
				restClientBuilder, STORAGE_URL, SERVICE_ROLE_KEY, BUCKET
		);
	}

	@Test
	void 업로드_성공시_영구_public_URL을_반환한다() {
		byte[] imageBytes = {1, 2, 3};
		String objectName = "sticker-diary-1.jpg";

		mockServer.expect(requestTo(
						STORAGE_URL + "/object/" + BUCKET + "/" + objectName
				))
				.andExpect(method(HttpMethod.PUT))
				.andExpect(header("Authorization", "Bearer " + SERVICE_ROLE_KEY))
				.andExpect(header("apikey", SERVICE_ROLE_KEY))
				.andExpect(header("x-upsert", "true"))
				.andExpect(content().contentType(MediaType.IMAGE_JPEG))
				.andExpect(content().bytes(imageBytes))
				.andRespond(withSuccess());

		String publicUrl = supabaseStorageClient.uploadFromBytes(imageBytes, objectName);

		assertEquals(
				STORAGE_URL + "/object/public/" + BUCKET + "/" + objectName,
				publicUrl
		);
		mockServer.verify();
	}

	@Test
	void 같은_objectName_재업로드를_허용하는_x_upsert_헤더를_보낸다() {
		byte[] imageBytes = {1, 2, 3};
		String objectName = "sticker-diary-1.jpg";

		mockServer.expect(requestTo(
						STORAGE_URL + "/object/" + BUCKET + "/" + objectName
				))
				.andExpect(header("x-upsert", "true"))
				.andRespond(withSuccess());

		supabaseStorageClient.uploadFromBytes(imageBytes, objectName);

		mockServer.verify();
	}

	@Test
	void 업로드_실패시_예외를_전파한다() {
		byte[] imageBytes = {1, 2, 3};
		String objectName = "sticker-diary-1.jpg";

		mockServer.expect(requestTo(
						STORAGE_URL + "/object/" + BUCKET + "/" + objectName
				))
				.andExpect(method(HttpMethod.PUT))
				.andRespond(withServerError());

		assertThrows(
				HttpServerErrorException.class,
				() -> supabaseStorageClient.uploadFromBytes(imageBytes, objectName)
		);
	}
}
