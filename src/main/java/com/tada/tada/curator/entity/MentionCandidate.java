package com.tada.tada.curator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "mention_candidate")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MentionCandidate {
	
	@Id
	private UUID id;
	
	@Column(name = "diary_id", nullable = false)
	private UUID diaryId;
	
	@Column(name = "raw_text", nullable = false)
	private String rawText;
	
	@Column(name = "normalized_text", nullable = false)
	private String normalizedText;
	
	@Column(name = "entity_type", nullable = false)
	private String entityType;
	
	@Column(name = "status", nullable = false)
	private String status;
	
	@Column(name = "matched_person_id")
	private UUID matchedPersonId;
	
	public static MentionCandidate create(
			UUID diaryId,
			String rawText,
			String normalizedText,
			String entityType,
			String status,
			UUID matchedPersonId
	) {
		MentionCandidate candidate = new MentionCandidate();
		
		candidate.id = UUID.randomUUID();
		candidate.diaryId = diaryId;
		candidate.rawText = rawText;
		candidate.normalizedText = normalizedText;
		candidate.entityType = entityType;
		candidate.status = status;
		candidate.matchedPersonId = matchedPersonId;
		
		return candidate;
	}
}