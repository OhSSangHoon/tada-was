package com.tada.tada.curator.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PersonCorrectionForm {
	
	private UUID targetPersonId;
	
	private String newDisplayName;
}