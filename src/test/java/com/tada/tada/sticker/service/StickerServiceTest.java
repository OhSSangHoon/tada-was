package com.tada.tada.sticker.service;

import com.tada.tada.diary.entity.Sticker;
import com.tada.tada.diary.entity.StickerType;
import com.tada.tada.diary.repository.StickerRepository;
import com.tada.tada.sticker.dto.StickerAlbumResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class StickerServiceTest {
	
	private StickerRepository stickerRepository;
	private StickerService stickerService;
	
	@BeforeEach
	void setUp() {
		stickerRepository = Mockito.mock(StickerRepository.class);
		
		stickerService = new StickerService(stickerRepository);
	}
	
	@Test
	void sort가_oldest면_오름차순으로_리포지토리를_호출한다() {
		UUID userId = UUID.randomUUID();
		
		when(stickerRepository.findByUserId(eq(userId), any(Sort.class)))
				.thenReturn(List.of());
		
		stickerService.getMyStickers(userId, "oldest");
		
		ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
		org.mockito.Mockito.verify(stickerRepository)
				.findByUserId(eq(userId), sortCaptor.capture());
		
		Sort usedSort = sortCaptor.getValue();
		assertTrue(usedSort.getOrderFor("createdAt").isAscending());
	}
	
	@Test
	void sort가_latest면_내림차순으로_리포지토리를_호출한다() {
		UUID userId = UUID.randomUUID();
		
		when(stickerRepository.findByUserId(eq(userId), any(Sort.class)))
				.thenReturn(List.of());
		
		stickerService.getMyStickers(userId, "latest");
		
		ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
		org.mockito.Mockito.verify(stickerRepository)
				.findByUserId(eq(userId), sortCaptor.capture());
		
		Sort usedSort = sortCaptor.getValue();
		assertTrue(usedSort.getOrderFor("createdAt").isDescending());
	}
	
	@Test
	void 스티커가_없으면_totalCount_0에_빈_리스트를_반환한다() {
		UUID userId = UUID.randomUUID();
		
		when(stickerRepository.findByUserId(eq(userId), any(Sort.class)))
				.thenReturn(List.of());
		
		StickerAlbumResponse result = stickerService.getMyStickers(userId, "latest");
		
		assertEquals(0, result.getTotalCount());
		assertEquals(0, result.getStickers().size());
	}
	
	@Test
	void 스티커가_있으면_totalCount와_stickers_리스트가_올바르게_채워진다() {
		UUID userId = UUID.randomUUID();
		UUID diaryId = UUID.randomUUID();
		UUID stickerId = UUID.randomUUID();
		
		Sticker sticker = createSticker(
				stickerId,
				diaryId,
				"https://example.com/sticker.png",
				"카페에서 여유로운 하루",
				StickerType.EXTRACTED,
				LocalDateTime.of(2026, 8, 1, 12, 0)
		);
		
		when(stickerRepository.findByUserId(eq(userId), any(Sort.class)))
				.thenReturn(List.of(sticker));
		
		StickerAlbumResponse result = stickerService.getMyStickers(userId, "latest");
		
		assertEquals(1, result.getTotalCount());
		assertEquals(1, result.getStickers().size());
		assertEquals(stickerId, result.getStickers().get(0).getId());
		assertEquals("카페에서 여유로운 하루", result.getStickers().get(0).getKeyword());
	}
	
	private Sticker createSticker(
			UUID id,
			UUID diaryId,
			String imageUrl,
			String keyword,
			StickerType type,
			LocalDateTime createdAt
	) {
		Sticker sticker = newInstance(Sticker.class);
		
		setField(sticker, "id", id);
		setField(sticker, "diaryId", diaryId);
		setField(sticker, "imageUrl", imageUrl);
		setField(sticker, "keyword", keyword);
		setField(sticker, "type", type);
		setField(sticker, "createdAt", createdAt);
		
		return sticker;
	}
	
	private <T> T newInstance(Class<T> clazz) {
		try {
			var constructor = clazz.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void setField(
			Object target,
			String fieldName,
			Object value
	) {
		try {
			Field field =
					target.getClass().getDeclaredField(fieldName);
			
			field.setAccessible(true);
			field.set(target, value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}