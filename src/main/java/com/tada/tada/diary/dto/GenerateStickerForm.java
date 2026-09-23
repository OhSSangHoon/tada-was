package com.tada.tada.diary.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerateStickerForm {
	@NotBlank
	private String keyword;
}
