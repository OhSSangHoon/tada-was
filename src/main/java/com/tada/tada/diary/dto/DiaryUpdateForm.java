package com.tada.tada.diary.dto;

import com.tada.tada.global.event.dto.ExtractionResult;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiaryUpdateForm {
	@NotBlank
	private String title;

	private String weather;

	@NotBlank
	private String content;

	// 본문이 바뀔 때 새로 추출한 인물/장소/활동.
	// null(필드 생략) = 재추출 없이 기존 추출 결과를 그대로 유지.
	// 빈 값({persons:[], places:[], activities:[]}) = Curator가 짝이 안 맞는 기존 후보를 전부 REMOVE 처리 → 기존 인물/장소/활동이 전부 삭제됨.
	// TODO: n8n 재추출 연동(5주차) 시 서버가 직접 추출하도록 바꾸고 이 필드는 제거
	private ExtractionResult extractionResult;
}
