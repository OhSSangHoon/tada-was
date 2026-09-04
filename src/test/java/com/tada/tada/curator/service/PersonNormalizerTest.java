package com.tada.tada.curator.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonNormalizerTest {
	
	private final PersonNormalizer personNormalizer =
			new PersonNormalizer();
	
	@Test
	void 받침이_없는_이름의_조사를_제거한다() {
		assertEquals(
				"민수",
				personNormalizer.normalizeName("민수와")
		);
	}
	
	@Test
	void 이름_뒤의_이와를_제거한다() {
		assertEquals(
				"한영",
				personNormalizer.normalizeName("한영이와")
		);
	}
	
	@Test
	void 이름_뒤의_이랑을_제거한다() {
		assertEquals(
				"지훈",
				personNormalizer.normalizeName("지훈이랑")
		);
	}
	
	@Test
	void 받침이_있는_이름의_은을_제거한다() {
		assertEquals(
				"한영",
				personNormalizer.normalizeName("한영은")
		);
	}
	
	@Test
	void 실제_이름의_은을_조사로_오인하지_않는다() {
		assertEquals(
				"박지은",
				personNormalizer.normalizeName("박지은")
		);
	}
	
	@Test
	void 긴_조사를_제거한다() {
		assertEquals(
				"영희",
				personNormalizer.normalizeName("영희에게")
		);
		
		assertEquals(
				"영희",
				personNormalizer.normalizeName("영희에게서")
		);
	}
	
	@Test
	void 도를_제거한다() {
		assertEquals(
				"민수",
				personNormalizer.normalizeName("민수도")
		);
	}
	
	@Test
	void 원형과_정규화된_이름을_후보로_만든다() {
		assertEquals(
				List.of("한영이와", "한영"),
				personNormalizer.normalizeCandidates("한영이와")
		);
	}
	
	@Test
	void 조사가_없으면_후보를_하나만_만든다() {
		assertEquals(
				List.of("철수"),
				personNormalizer.normalizeCandidates("철수")
		);
	}
	
	@Test
	void 공백과_구두점을_정리한다() {
		assertEquals(
				"민수",
				personNormalizer.normalizeName("  민수와!!!  ")
		);
	}
	
	@Test
	void 빈_값은_빈_결과를_반환한다() {
		assertEquals(
				"",
				personNormalizer.normalizeName(" ")
		);
		
		assertEquals(
				List.of(),
				personNormalizer.normalizeCandidates(" ")
		);
		
		assertEquals(
				"",
				personNormalizer.normalizeName(null)
		);
	}
}