package com.tada.tada.curator.validation;

import com.tada.tada.curator.service.PersonNormalizer;
import com.tada.tada.global.event.dto.PersonExtraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonExtractionFilterTest {
	
	private PersonExtractionFilter filter;
	
	@BeforeEach
	void setUp() {
		filter =
				new PersonExtractionFilter(
						new PersonNormalizer()
				);
	}
	
	@Test
	void 집단과_불특정_인물_표현은_제외한다() {
		for (String rawText : List.of(
				"가족",
				"가족들",
				"가족들과",
				"친구들",
				"친구들과",
				"동네친구들",
				"동네친구들과",
				"학교친구들",
				"학교친구들과",
				"회사동료들",
				"회사동료들과",
				"개발팀원들",
				"사람들",
				"동네사람들",
				"선배들",
				"학교선배들",
				"후배들",
				"회사후배들",
				"지인들",
				"동네주민들",
				"여러 명",
				"세 명",
				"3명",
				"우리 가족",
				"우리 팀",
				"그 사람",
				"다른 친구"
		)) {
			assertTrue(
					filter.shouldExclude(rawText),
					rawText
			);
		}
	}
	
	@Test
	void 특정_한_사람을_가리킬_수_있는_표현은_유지한다() {
		for (String rawText : List.of(
				"엄마",
				"아빠",
				"형",
				"누나",
				"동생",
				"친구",
				"동네친구",
				"선생님",
				"아들",
				"김사랑",
				"이영도",
				"김성은",
				"박지은",
				"가을이",
				"도영"
		)) {
			assertFalse(
					filter.shouldExclude(rawText),
					rawText
			);
		}
	}
	
	@Test
	void 제외된_PERSON의_ref만_반환한다() {
		Set<String> result =
				filter.findExcludedRefs(
						List.of(
								new PersonExtraction(
										"p1",
										"동네친구들과",
										"PERSON"
								),
								new PersonExtraction(
										"p2",
										"엄마",
										"PERSON"
								),
								new PersonExtraction(
										"p3",
										"회사동료들",
										"PERSON"
								)
						)
				);
		
		assertEquals(
				Set.of("p1", "p3"),
				result
		);
	}
}