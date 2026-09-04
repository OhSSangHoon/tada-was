package com.tada.tada.diary.dto;

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
}
