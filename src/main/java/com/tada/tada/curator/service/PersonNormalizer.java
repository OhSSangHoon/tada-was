package com.tada.tada.curator.service;

import com.tada.tada.curator.model.PersonNormalization;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class PersonNormalizer {

	public PersonNormalization normalize(String rawText) {
		if (rawText == null || rawText.isBlank()) {
			return emptyNormalization();
		}

		String cleanedText = cleanText(rawText);

		if (cleanedText.isBlank()) {
			return emptyNormalization();
		}

		String normalizedText = removeStrongParticle(cleanedText);

		Set<String> strongMatchCandidates = new LinkedHashSet<>();
		strongMatchCandidates.add(cleanedText);
		strongMatchCandidates.add(normalizedText);

		Set<String> weakMatchCandidates = new LinkedHashSet<>();

		addWeakParticleCandidates(
				weakMatchCandidates,
				normalizedText
		);

		addHonorificCandidate(
				weakMatchCandidates,
				normalizedText
		);

		for (String candidate : List.copyOf(weakMatchCandidates)) {
			addHonorificCandidate(
					weakMatchCandidates,
					candidate
			);
		}

		weakMatchCandidates.removeAll(strongMatchCandidates);

		return new PersonNormalization(
				normalizedText,
				List.copyOf(strongMatchCandidates),
				List.copyOf(weakMatchCandidates)
		);
	}

	public String normalizeName(String rawText) {
		return normalize(rawText).normalizedText();
	}

	public List<String> normalizeCandidates(String rawText) {
		return normalize(rawText).strongMatchCandidates();
	}

	private PersonNormalization emptyNormalization() {
		return new PersonNormalization(
				"",
				List.of(),
				List.of()
		);
	}

	private String cleanText(String rawText) {
		String normalized = Normalizer.normalize(
				rawText,
				Normalizer.Form.NFKC
		);

		return normalized
				.trim()
				.replaceAll("\\s+", " ")
				.replaceAll(
						"^[\\p{Punct}]+|[\\p{Punct}]+$",
						""
				);
	}

	private String removeStrongParticle(String text) {
		String result;

		result = removeSimpleParticle(text, "에게서");
		if (!result.equals(text)) return result;

		result = removeSimpleParticle(text, "한테서");
		if (!result.equals(text)) return result;

		result = removeSimpleParticle(text, "에게");
		if (!result.equals(text)) return result;

		result = removeSimpleParticle(text, "한테");
		if (!result.equals(text)) return result;

		result = removeSimpleParticle(text, "하고");
		if (!result.equals(text)) return result;

		result = removeParticleWithBatchim(text, "이와", true);
		if (!result.equals(text)) return result;

		result = removeParticleWithBatchim(text, "이랑", true);
		if (!result.equals(text)) return result;

		result = removeParticleWithBatchim(text, "이가", true);
		if (!result.equals(text)) return result;

		result = removeParticleWithBatchim(text, "이는", true);
		if (!result.equals(text)) return result;

		result = removeParticleWithBatchim(text, "이를", true);
		if (!result.equals(text)) return result;

		result = removeParticleWithBatchim(text, "와", false);
		if (!result.equals(text)) return result;

		result = removeParticleWithBatchim(text, "랑", false);
		if (!result.equals(text)) return result;

		result = removeParticleWithBatchim(text, "를", false);
		if (!result.equals(text)) return result;

		result = removeParticleWithBatchim(text, "과", true);
		if (!result.equals(text)) return result;

		result = removeParticleWithBatchim(text, "을", true);
		if (!result.equals(text)) return result;

		return removeSimpleParticle(text, "도");
	}

	private void addWeakParticleCandidates(
			Set<String> weakMatchCandidates,
			String text
	) {
		addWeakParticleCandidate(
				weakMatchCandidates,
				text,
				"은",
				true
		);

		addWeakParticleCandidate(
				weakMatchCandidates,
				text,
				"는",
				false
		);

		addWeakParticleCandidate(
				weakMatchCandidates,
				text,
				"이",
				true
		);

		addWeakParticleCandidate(
				weakMatchCandidates,
				text,
				"가",
				false
		);
	}

	private void addWeakParticleCandidate(
			Set<String> weakMatchCandidates,
			String text,
			String particle,
			boolean requiresBatchim
	) {
		String candidate = removeParticleWithBatchim(
				text,
				particle,
				requiresBatchim
		);

		if (!candidate.equals(text)) {
			weakMatchCandidates.add(candidate);
		}
	}

	private void addHonorificCandidate(
			Set<String> weakMatchCandidates,
			String text
	) {
		addSuffixRemovedCandidate(
				weakMatchCandidates,
				text,
				"님"
		);

		addSuffixRemovedCandidate(
				weakMatchCandidates,
				text,
				"씨"
		);
	}

	private void addSuffixRemovedCandidate(
			Set<String> candidates,
			String text,
			String suffix
	) {
		if (!text.endsWith(suffix)) {
			return;
		}

		String candidate = text.substring(
				0,
				text.length() - suffix.length()
		).trim();

		if (candidate.length() >= 2) {
			candidates.add(candidate);
		}
	}

	private String removeSimpleParticle(
			String text,
			String particle
	) {
		if (!text.endsWith(particle)) {
			return text;
		}

		String base = text.substring(
				0,
				text.length() - particle.length()
		);

		return base.length() >= 2
				? base
				: text;
	}

	private String removeParticleWithBatchim(
			String text,
			String particle,
			boolean requiresBatchim
	) {
		if (!text.endsWith(particle)) {
			return text;
		}

		String base = text.substring(
				0,
				text.length() - particle.length()
		);

		if (base.length() < 2) {
			return text;
		}

		if (hasBatchim(base) != requiresBatchim) {
			return text;
		}

		return base;
	}

	private boolean hasBatchim(String text) {
		char lastChar = text.charAt(text.length() - 1);

		if (lastChar < '가' || lastChar > '힣') {
			return false;
		}

		return (lastChar - '가') % 28 != 0;
	}
}