package com.tada.tada.curator.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

@Component
public class PersonNormalizer {
	
	public String normalizeName(String rawText) {
		if (rawText == null || rawText.isBlank()) {
			return "";
		}
		
		String cleanedText = cleanText(rawText);
		
		if (cleanedText.isBlank()) {
			return "";
		}
		
		return removeParticle(cleanedText);
	}
	
	public List<String> normalizeCandidates(String rawText) {
		if (rawText == null || rawText.isBlank()) {
			return List.of();
		}
		
		String cleanedText = cleanText(rawText);
		
		if (cleanedText.isBlank()) {
			return List.of();
		}
		
		String normalizedName = removeParticle(cleanedText);
		
		List<String> candidates = new ArrayList<>();
		candidates.add(cleanedText);
		
		if (!cleanedText.equals(normalizedName)) {
			candidates.add(normalizedName);
		}
		
		return candidates;
	}
	
	private String cleanText(String rawText) {
		String normalized = Normalizer.normalize(
				rawText,
				Normalizer.Form.NFKC
		);
		
		return normalized
				.trim()
				.replaceAll("\\s+", " ")
				.replaceAll("^[\\p{Punct}]+|[\\p{Punct}]+$", "");
	}
	
	private String removeParticle(String text) {
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
		
		result = removeParticleWithBatchim(text, "는", false);
		if (!result.equals(text)) return result;
		
		result = removeParticleWithBatchim(text, "가", false);
		if (!result.equals(text)) return result;
		
		result = removeParticleWithBatchim(text, "를", false);
		if (!result.equals(text)) return result;
		
		result = removeParticleWithBatchim(text, "과", true);
		if (!result.equals(text)) return result;
		
		result = removeParticleWithBatchim(text, "은", true);
		if (!result.equals(text)) return result;
		
		result = removeParticleWithBatchim(text, "이", true);
		if (!result.equals(text)) return result;
		
		result = removeParticleWithBatchim(text, "을", true);
		if (!result.equals(text)) return result;
		
		return removeSimpleParticle(text, "도");
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
		
		return base.length() >= 2 ? base : text;
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