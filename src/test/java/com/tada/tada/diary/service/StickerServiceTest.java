package com.tada.tada.diary.service;

import com.tada.tada.diary.dto.StickerResponse;
import com.tada.tada.diary.dto.StickerSortOption;
import com.tada.tada.diary.entity.DiaryStatus;
import com.tada.tada.diary.entity.Sticker;
import com.tada.tada.diary.entity.StickerType;
import com.tada.tada.diary.repository.StickerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
		
		when(stickerRepository.findByUserId(eq(userId), eq(DiaryStatus.ACTIVE), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of()));
		
		stickerService.getMyStickers(userId, StickerSortOption.OLDEST, 0, 12);
		
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(stickerRepository).findByUserId(eq(userId), eq(DiaryStatus.ACTIVE), pageableCaptor.capture());
		
		Pageable usedPageable = pageableCaptor.getValue();
		assertTrue(usedPageable.getSort().getOrderFor("createdAt").isAscending());
	}
	
	@Test
	void sort가_latest면_내림차순으로_리포지토리를_호출한다() {
		UUID userId = UUID.randomUUID();
		
		when(stickerRepository.findByUserId(eq(userId), eq(DiaryStatus.ACTIVE), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of()));
		
		stickerService.getMyStickers(userId, StickerSortOption.LATEST, 0, 12);
		
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(stickerRepository).findByUserId(eq(userId), eq(DiaryStatus.ACTIVE), pageableCaptor.capture());
		
		Pageable usedPageable = pageableCaptor.getValue();
		assertTrue(usedPageable.getSort().getOrderFor("createdAt").isDescending());
	}
	
	@Test
	void 스티커가_없으면_totalElements_0에_빈_페이지를_반환한다() {
		UUID userId = UUID.randomUUID();
		
		when(stickerRepository.findByUserId(eq(userId), eq(DiaryStatus.ACTIVE), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of()));
		
		Page<StickerResponse> result = stickerService.getMyStickers(userId, StickerSortOption.LATEST, 0, 12);
		
		assertEquals(0, result.getTotalElements());
		assertTrue(result.getContent().isEmpty());
	}
	
	@Test
	void 스티커가_있으면_Page_안에_매핑된_StickerResponse가_담긴다() {
		UUID userId = UUID.randomUUID();
		UUID diaryId = UUID.randomUUID();
		Sticker sticker = createSticker(UUID.randomUUID(), diaryId, "img.png", "행복", StickerType.EXTRACTED, LocalDateTime.now());
		
		Pageable pageable = PageRequest.of(0, 12);
		when(stickerRepository.findByUserId(eq(userId), eq(DiaryStatus.ACTIVE), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(sticker), pageable, 1));
		
		Page<StickerResponse> result = stickerService.getMyStickers(userId, StickerSortOption.LATEST, 0, 12);
		
		assertEquals(1, result.getTotalElements());
		assertEquals("행복", result.getContent().get(0).getKeyword());
	}
	
	private Sticker createSticker(UUID id, UUID diaryId, String imageUrl, String keyword,
								  StickerType type, LocalDateTime createdAt) {
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
	
	private void setField(Object target, String fieldName, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}