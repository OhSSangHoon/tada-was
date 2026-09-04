package com.tada.tada.global.event.dto;

public record PersonExtraction(
        String ref,
        String rawText,
        String entityType
) {
}
