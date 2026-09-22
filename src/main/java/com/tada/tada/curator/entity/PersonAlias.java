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
@Table(name = "person_alias")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonAlias {

	@Id
	private UUID id;

	@Column(name = "person_id", nullable = false)
	private UUID personId;

	@Column(name = "owner_user_id", nullable = false)
	private UUID ownerUserId;

	@Column(name = "alias_text", nullable = false)
	private String aliasText;

	@Column(name = "normalized_text", nullable = false)
	private String normalizedText;

	public static PersonAlias create(
			UUID personId,
			UUID ownerUserId,
			String aliasText,
			String normalizedText
	) {
		if (personId == null) {
			throw new IllegalArgumentException(
					"personId must not be null"
			);
		}

		if (ownerUserId == null) {
			throw new IllegalArgumentException(
					"ownerUserId must not be null"
			);
		}

		if (aliasText == null
				|| aliasText.isBlank()) {
			throw new IllegalArgumentException(
					"aliasText must not be blank"
			);
		}

		if (normalizedText == null
				|| normalizedText.isBlank()) {
			throw new IllegalArgumentException(
					"normalizedText must not be blank"
			);
		}

		PersonAlias alias = new PersonAlias();

		alias.id = UUID.randomUUID();
		alias.personId = personId;
		alias.ownerUserId = ownerUserId;
		alias.aliasText = aliasText;
		alias.normalizedText = normalizedText;

		return alias;
	}
}
