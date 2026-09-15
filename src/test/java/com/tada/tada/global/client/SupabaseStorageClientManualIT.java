package com.tada.tada.global.client;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * 수동 통합 테스트 — 실제 n8n 웹훅과 실제 Supabase Storage를 호출한다.
 * @Tag("manual")로 표시되어 기본 test 태스크에서는 제외된다 (build.gradle 참고).
 *
 * 실행 전: n8n 워크플로우 편집기에서 "Listen for test event"를 눌러 webhook-test를 활성화할 것.
 * 실행: ./gradlew manualTest
 *
 * 확인 끝나면 이 파일은 지워도 된다.
 */
@Tag("manual")
@SpringBootTest
class SupabaseStorageClientManualIT {

	@Autowired
	private SupabaseStorageClient supabaseStorageClient;

	@Value("${STICKER_WEBHOOK_URL}")
	private String webhookUrl;

	@Test
	void 키워드로_스티커_이미지를_생성하고_supabase에_업로드한다() {
		RestClient restClient = RestClient.create();

		byte[] imageBytes = restClient.post()
				.uri(webhookUrl)
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"keyword\": \"영화\"}")
				.retrieve()
				.body(byte[].class);

		assertNotNull(imageBytes);
		assertTrue(imageBytes.length > 0, "n8n 응답 바이트가 비어있음");

		String objectName = "manual-test-" + UUID.randomUUID() + ".jpg";
		String publicUrl = supabaseStorageClient.uploadFromBytes(imageBytes, objectName);

		System.out.println("업로드된 이미지 URL: " + publicUrl);

		assertTrue(publicUrl.startsWith("https://"));
	}
}
