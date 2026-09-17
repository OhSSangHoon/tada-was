package com.tada.tada.global.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

/*
 * [담당: 상훈] — 스티커 이미지 바이트를 Supabase Storage에 업로드하고 영구 public URL을 돌려준다.
 *
 * 연결 구조:
 *   민혁의 스티커 생성 트리거 엔드포인트(diary 도메인)
 *     --> n8n Webhook을 동기 호출 (Webhook -> ... -> Respond to Webhook)
 *         n8n의 "Respond to Webhook" 노드가 "Respond with: Binary File"로 설정돼 있어서
 *         응답 자체가 이미지 파일(Content-Type: image/jpeg)이다.
 *         즉 RestClient로 이 웹훅을 호출하면 .retrieve().body(byte[].class)로
 *         이미지 바이트를 바로 받을 수 있다 (base64 디코딩 불필요).
 *     --> 그 byte[]를 이 클라이언트에 넘겨서 업로드
 *     --> 돌아온 영구 URL을 Sticker.imageUrl로 저장
 *
 * Supabase Storage REST API (참고 — 실제 응답 보고 조정 필요할 수 있음):
 *   업로드: PUT {SUPABASE_URL}/storage/v1/object/{bucket}/{objectName}
 *     헤더: Authorization: Bearer {service_role_key}, apikey: {service_role_key}
 *     body: 이미지 바이트, Content-Type: image/jpeg
 *   영구 URL (버킷이 public일 때):
 *     {SUPABASE_URL}/storage/v1/object/public/{bucket}/{objectName}
 *
 * 설정값 출처: application.yaml의 supabase.storage.*
 *   (= .env.local의 SUPABASE_SERVICE_ROLE_KEY, SUPABASE_STICKER_BUCKET)
 */
@Slf4j
@Component
public class SupabaseStorageClient {

	private final RestClient restClient;
	private final String storageUrl;
	private final String serviceRoleKey;
	private final String bucket;

	public SupabaseStorageClient(
			RestClient.Builder restClientBuilder,
			@Value("${supabase.storage.url}") String storageUrl,
			@Value("${supabase.storage.service-role-key}") String serviceRoleKey,
			@Value("${supabase.storage.bucket}") String bucket
	) {
		this.storageUrl = trimTrailingSlash(storageUrl);
		this.serviceRoleKey = serviceRoleKey;
		this.bucket = bucket;
		this.restClient = restClientBuilder.build();
	}

	/**
	 * 이미지 바이트(JPEG)를 Supabase Storage에 업로드하고 영구 public URL을 반환한다.
	 */
	public String uploadFromBytes(byte[] imageBytes, String objectName) {
		String encodedBucket = UriUtils.encodePathSegment(bucket, StandardCharsets.UTF_8);
		String encodedObjectName = encodeObjectPath(objectName);

		restClient.put()
				.uri(URI.create(storageUrl + "/object/" + encodedBucket + "/" + encodedObjectName))
				.header("Authorization", "Bearer " + serviceRoleKey)
				.header("apikey", serviceRoleKey)
				.header("x-upsert", "true")
				.contentType(MediaType.IMAGE_JPEG)
				.body(imageBytes)
				.retrieve()
				.toBodilessEntity();

		return storageUrl + "/object/public/" + encodedBucket + "/" + encodedObjectName;
	}

	/*
	 * objectName은 "userId/diaryId.jpg"처럼 '/'로 구분된 경로일 수 있으므로,
	 * 세그먼트 단위로 인코딩해 '/'는 구분자로 보존하고 나머지 특수문자만 percent-encoding한다.
	 * 업로드 요청 URI와 반환 public URL이 동일한 인코딩 결과를 공유해야 경로가 일치한다.
	 */
	private static String encodeObjectPath(String objectName) {
		return Arrays.stream(objectName.split("/", -1))
				.map(segment -> UriUtils.encodePathSegment(segment, StandardCharsets.UTF_8))
				.collect(Collectors.joining("/"));
	}

	private static String trimTrailingSlash(String url) {
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}
}
