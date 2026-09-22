package com.tada.tada.diary.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CanCreateResponse {
	private boolean canCreate;
	private String reason;
}
