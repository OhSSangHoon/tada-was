package com.tada.tada.diary.service;

import com.tada.tada.diary.repository.StickerRepository;
import com.tada.tada.global.client.StickerObjectCleanupClient;
import com.tada.tada.global.client.StickerObjectCleanupClient.StorageObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StickerCleanupServiceTest {

	private StickerObjectCleanupClient stickerObjectCleanupClient;
	private StickerRepository stickerRepository;
	private StickerCleanupService stickerCleanupService;

	@BeforeEach
	void setUp() {
		stickerObjectCleanupClient = Mockito.mock(StickerObjectCleanupClient.class);
		stickerRepository = Mockito.mock(StickerRepository.class);
		stickerCleanupService = new StickerCleanupService(stickerObjectCleanupClient, stickerRepository);
	}

	@Test
	void 참조도_안되고_유예시간도_지난_오브젝트만_지운다() {
		Instant old = Instant.now().minus(48, ChronoUnit.HOURS);
		when(stickerRepository.findAllImageUrls()).thenReturn(List.of());
		when(stickerObjectCleanupClient.listAllObjects())
				.thenReturn(List.of(new StorageObject("user-1/orphan.jpg", old)));

		stickerCleanupService.cleanupOrphanedObjects();

		verify(stickerObjectCleanupClient).deleteObjects(List.of("user-1/orphan.jpg"));
	}

	@Test
	void DB에_참조된_오브젝트는_오래됐어도_지우지_않는다() {
		Instant old = Instant.now().minus(48, ChronoUnit.HOURS);
		when(stickerRepository.findAllImageUrls())
				.thenReturn(List.of("https://x.supabase.co/storage/v1/object/public/stickers/user-1/kept.jpg"));
		when(stickerObjectCleanupClient.listAllObjects())
				.thenReturn(List.of(new StorageObject("user-1/kept.jpg", old)));

		stickerCleanupService.cleanupOrphanedObjects();

		verify(stickerObjectCleanupClient, never()).deleteObjects(Mockito.any());
	}

	@Test
	void 유예시간이_안_지난_미참조_오브젝트는_지우지_않는다() {
		Instant recent = Instant.now().minus(1, ChronoUnit.HOURS);
		when(stickerRepository.findAllImageUrls()).thenReturn(List.of());
		when(stickerObjectCleanupClient.listAllObjects())
				.thenReturn(List.of(new StorageObject("user-1/fresh.jpg", recent)));

		stickerCleanupService.cleanupOrphanedObjects();

		verify(stickerObjectCleanupClient, never()).deleteObjects(Mockito.any());
	}

	@Test
	void 삭제_대상이_없으면_삭제_호출_자체를_안한다() {
		when(stickerRepository.findAllImageUrls()).thenReturn(List.of());
		when(stickerObjectCleanupClient.listAllObjects()).thenReturn(List.of());

		stickerCleanupService.cleanupOrphanedObjects();

		verify(stickerObjectCleanupClient, never()).deleteObjects(Mockito.any());
	}
}
