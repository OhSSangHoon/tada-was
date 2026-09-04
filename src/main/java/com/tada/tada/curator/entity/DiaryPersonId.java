package com.tada.tada.curator.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DiaryPersonId implements Serializable {
	
	private UUID diaryId;
	private UUID personId;
}
