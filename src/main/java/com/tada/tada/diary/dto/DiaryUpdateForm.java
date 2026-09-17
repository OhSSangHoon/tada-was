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

	// 본문(content)이 실제로 바뀌는 요청일 때만 필수. 제목/날씨만 수정할 땐 null이어도 됨.
	// TODO: 지금은 n8n 재추출 연동이 없어서 프론트/Postman이 직접 넣어주는 값을 그대로 씀 (createDiary와 동일 패턴) — 5주차 n8n 연동 시 교체
	private ExtractionResult extractionResult;
}
