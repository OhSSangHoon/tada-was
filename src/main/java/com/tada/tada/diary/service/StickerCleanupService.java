package com.tada.tada.diary.service;

import com.tada.tada.diary.repository.StickerRepository;
import com.tada.tada.global.client.StickerObjectCleanupClient;
import com.tada.tada.global.client.StickerObjectCleanupClient.StorageObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * [담당: 민혁] generate-sticker/regenerate-sticker를 호출할 때마다 Supabase Storage에 새
 * 오브젝트가 쌓이는데, 사용자가 최종 확인(POST /api/diaries)까지 안 가고 끝내면 그 오브젝트는
 * DB 어디에도 참조가 안 남아 계속 쌓이기만 한다 (상훈 PR #32 리뷰 반영).
 *
 * DB의 stickers.image_url에 참조되지 않고, 생성된 지 일정 시간(GRACE_PERIOD) 지난 오브젝트만
 * 정리 대상으로 삼는다. 생성 즉시 안 지우는 이유: 사용자가 스티커 생성 직후 아직 확인
 * 화면에 머무는 중일 수도 있어서(POST /api/diaries로 저장하기 전까지는 항상 미참조 상태).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StickerCleanupService {

	private static final Duration GRACE_PERIOD = Duration.ofHours(24);

	private final StickerObjectCleanupClient stickerObjectCleanupClient;
	private final StickerRepository stickerRepository;

	public void cleanupOrphanedObjects() {
		Set<String> referencedPaths = stickerRepository.findAllImageUrls().stream()
				.map(StickerCleanupService::extractObjectPath)
				.collect(Collectors.toSet());

		Instant cutoff = Instant.now().minus(GRACE_PERIOD);

		List<String> orphanedPaths = stickerObjectCleanupClient.listAllObjects().stream()
				.filter(object -> object.createdAt().isBefore(cutoff))
				.map(StorageObject::path)
				.filter(path -> !referencedPaths.contains(path))
				.toList();

		if (orphanedPaths.isEmpty()) {
			return;
		}

		log.info("미참조 스티커 오브젝트 {}개 정리", orphanedPaths.size());
		stickerObjectCleanupClient.deleteObjects(orphanedPaths);
	}

	// image_url은 ".../object/public/{bucket}/{userId}/{file}.jpg" 형태의 영구 URL이라,
	// storage 목록 API가 주는 상대 경로("userId/file.jpg")와 비교하려면 마지막 두 세그먼트만 꺼내야 한다.
	private static String extractObjectPath(String imageUrl) {
		String[] segments = imageUrl.split("/");
		return segments[segments.length - 2] + "/" + segments[segments.length - 1];
	}
}
