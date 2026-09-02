package com.tada.tada.diary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DiaryCreateForm {
	@NotNull
	private LocalDate entryDate;
	
	@NotBlank
	private String title;
	
	private String weather;
	
	@NotBlank
	private String content;
}
