package com.tada.tada.global.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class StickerObjectCleanupClientTest {

	private static final String STORAGE_URL = "https://example.supabase.co/storage/v1";
	private static final String BUCKET = "stickers";

	private MockRestServiceServer mockServer;
	private StickerObjectCleanupClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		mockServer = MockRestServiceServer.bindTo(builder).build();
		client = new StickerObjectCleanupClient(builder, STORAGE_URL, "service-role-key", BUCKET);
	}

	@Test
	void 루트에서_유저_폴더를_찾고_그_안의_파일만_모아_전체_오브젝트_목록을_만든다() {
		mockServer.expect(requestTo(STORAGE_URL + "/object/list/" + BUCKET))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().json("{\"prefix\":\"\",\"limit\":1000,\"offset\":0}"))
				.andRespond(withSuccess("""
						[
						  {"name": "user-1", "id": null, "created_at": null}
						]
						""", MediaType.APPLICATION_JSON));

		mockServer.expect(requestTo(STORAGE_URL + "/object/list/" + BUCKET))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().json("{\"prefix\":\"user-1/\",\"limit\":1000,\"offset\":0}"))
				.andRespond(withSuccess("""
						[
						  {"name": "a.jpg", "id": "obj-1", "created_at": "2026-09-20T00:00:00Z"},
						  {"name": "sub-folder", "id": null, "created_at": null}
						]
						""", MediaType.APPLICATION_JSON));

		List<StickerObjectCleanupClient.StorageObject> objects = client.listAllObjects();

		assertEquals(1, objects.size());
		assertEquals("user-1/a.jpg", objects.get(0).path());
		mockServer.verify();
	}

	@Test
	void 오브젝트_경로_목록으로_삭제_요청을_보낸다() {
		mockServer.expect(requestTo(STORAGE_URL + "/object/" + BUCKET))
				.andExpect(method(HttpMethod.DELETE))
				.andExpect(content().json("{\"prefixes\":[\"user-1/a.jpg\"]}"))
				.andRespond(withSuccess());

		client.deleteObjects(List.of("user-1/a.jpg"));

		mockServer.verify();
	}

	@Test
	void 삭제할_경로가_없으면_요청_자체를_보내지_않는다() {
		// mockServer에 아무 expect()도 안 걸어뒀으니, 요청이 실제로 나가면
		// MockRestServiceServer가 "예상치 못한 요청"으로 예외를 던진다.
		assertDoesNotThrow(() -> client.deleteObjects(List.of()));
		mockServer.verify();
	}
}
