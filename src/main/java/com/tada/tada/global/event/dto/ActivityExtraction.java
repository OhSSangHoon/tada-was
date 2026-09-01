package com.tada.tada.global.event.dto;

import java.util.List;

public record ActivityExtraction(
        String rawText,
        String normalizedText,
        String entityType,
        List<String> personRefs
) {
}
