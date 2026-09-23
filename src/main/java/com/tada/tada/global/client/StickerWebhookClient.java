package com.tada.tada.global.client;

import com.tada.tada.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/*
 * [담당: 민혁] — keyword로 n8n 스티커 생성 웹훅을 동기 호출해 이미지 바이트를 받아온다.
 *
 * n8n의 "Respond to Webhook" 노드가 "Respond with: Binary File"로 설정돼 있어서
 * 응답 자체가 이미지 파일(Content-Type: image/jpeg)이다 — base64 디코딩 불필요.
 * (검증 근거: SupabaseStorageClientManualIT 수동 테스트로 실제 n8n + Supabase 연동 확인 완료)
 *
 * 재생성(regenerate)도 별도 엔드포인트 없이 같은 keyword로 이 웹훅을 한 번 더 호출하면 된다.
 */
@Slf4j
@Component
public class StickerWebhookClient {

	private final RestClient restClient;
	private final String webhookUrl;

	public StickerWebhookClient(
			RestClient.Builder restClientBuilder,
			@Value("${n8n.sticker-webhook-url}") String webhookUrl
	) {
		this.restClient = restClientBuilder.build();
		this.webhookUrl = webhookUrl;
	}

	public byte[] requestStickerImage(String keyword) {
		try {
			byte[] imageBytes = restClient.post()
					.uri(webhookUrl)
					.contentType(MediaType.APPLICATION_JSON)
					.body(new StickerWebhookRequest(keyword))
					.retrieve()
					.body(byte[].class);

			if (imageBytes == null || imageBytes.length == 0) {
				throw new CustomException("스티커 생성에 실패했습니다. 다시 시도해주세요.", 502);
			}
			return imageBytes;
		} catch (RestClientException e) {
			log.error("스티커 생성 웹훅 호출 실패 (keyword={})", keyword, e);
			throw new CustomException("스티커 생성에 실패했습니다. 다시 시도해주세요.", 502);
		}
	}

	private record StickerWebhookRequest(String keyword) {
	}
}
