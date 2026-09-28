package com.tada.tada.global.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * [담당: 민혁] — 미참조 스티커 오브젝트 정리 배치(StickerCleanupService) 전용 Supabase Storage 클라이언트.
 *
 * SupabaseStorageClient([담당: 상훈], 업로드 전용)는 건드리지 않고, 같은 supabase.storage.*
 * 설정값만 그대로 재사용해서 "목록 조회"와 "삭제"를 별도 클래스로 분리 구현했다
 * - 관할 아닌 파일은 직접 수정하지 않는다는 팀 컨벤션에 따름.
 *
 * Supabase Storage에는 진짜 폴더가 없고 오브젝트 경로의 prefix로 흉내만 낸다. list API는
 * 한 단계(prefix) 안의 항목만 돌려주기 때문에, 루트에서 "폴더"(=userId) 목록을 먼저 받고
 * 그 안을 한 번 더 조회하는 2단계로 전체 오브젝트를 모은다. 우리 오브젝트 경로가
 * "userId/uuid.jpg"로 딱 한 단계 깊이라 2단계 조회면 충분하다.
 * (list 응답에서 폴더 항목은 id가 null, 파일 항목은 id가 있음 - Supabase Storage list API 규약)
 */
@Component
public class StickerObjectCleanupClient {

	private static final int LIST_PAGE_SIZE = 1000;
	private static final int DELETE_CHUNK_SIZE = 100;

	private final RestClient restClient;
	private final String storageUrl;
	private final String serviceRoleKey;
	private final String bucket;

	public StickerObjectCleanupClient(
			RestClient.Builder restClientBuilder,
			@Value("${supabase.storage.url}") String storageUrl,
			@Value("${supabase.storage.service-role-key}") String serviceRoleKey,
			@Value("${supabase.storage.bucket}") String bucket
	) {
		this.restClient = restClientBuilder.build();
		this.storageUrl = trimTrailingSlash(storageUrl);
		this.serviceRoleKey = serviceRoleKey;
		this.bucket = bucket;
	}

	/**
	 * 버킷 안의 모든 오브젝트를 "userId/파일명" 상대 경로 + 생성시각으로 반환한다.
	 */
	public List<StorageObject> listAllObjects() {
		List<StorageObject> result = new ArrayList<>();
		for (RawItem folder : list("")) {
			if (folder.id() != null) {
				continue;
			}
			String userId = folder.name();
			for (RawItem file : list(userId + "/")) {
				if (file.id() == null) {
					continue;
				}
				result.add(new StorageObject(userId + "/" + file.name(), file.createdAt()));
			}
		}
		return result;
	}

	/**
	 * 상대 경로("userId/파일명") 목록을 받아 한 번에 삭제한다.
	 */
	public void deleteObjects(List<String> objectPaths) {
		// 한 요청에 너무 많은 경로를 실으면 요청 크기/처리 시간 제한에 걸릴 수 있어 나눠서 보낸다
		for (int from = 0; from < objectPaths.size(); from += DELETE_CHUNK_SIZE) {
			int to = Math.min(from + DELETE_CHUNK_SIZE, objectPaths.size());
			restClient.method(HttpMethod.DELETE)
					.uri(storageUrl + "/object/" + bucket)
					.header("Authorization", "Bearer " + serviceRoleKey)
					.header("apikey", serviceRoleKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(new DeleteRequest(objectPaths.subList(from, to)))
					.retrieve()
					.toBodilessEntity();
		}
	}

	// list API는 한 번에 최대 LIST_PAGE_SIZE개만 주므로, 꽉 찬 페이지가 오는 동안 offset을 밀며 끝까지 읽는다.
	// 페이지 사이에 순서가 흔들리지 않게 이름순으로 고정한다.
	private List<RawItem> list(String prefix) {
		List<RawItem> all = new ArrayList<>();
		int offset = 0;
		while (true) {
			RawItem[] page = restClient.post()
					.uri(storageUrl + "/object/list/" + bucket)
					.header("Authorization", "Bearer " + serviceRoleKey)
					.header("apikey", serviceRoleKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(new ListRequest(prefix, LIST_PAGE_SIZE, offset, new SortBy("name", "asc")))
					.retrieve()
					.body(RawItem[].class);

			if (page == null) {
				break;
			}
			all.addAll(Arrays.asList(page));
			if (page.length < LIST_PAGE_SIZE) {
				break;
			}
			offset += LIST_PAGE_SIZE;
		}
		return all;
	}

	private static String trimTrailingSlash(String url) {
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}

	public record StorageObject(String path, Instant createdAt) {
	}

	private record SortBy(String column, String order) {
	}

	private record ListRequest(String prefix, int limit, int offset, SortBy sortBy) {
	}

	private record DeleteRequest(List<String> prefixes) {
	}

	private record RawItem(String name, String id, @JsonProperty("created_at") Instant createdAt) {
	}
}
