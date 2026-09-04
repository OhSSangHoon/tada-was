package com.tada.tada.global.event;

import com.tada.tada.global.event.dto.ExtractionResult;

import java.util.UUID;

public record MentionExtractedEvent(
        UUID diaryId,
        UUID userId,
        ExtractionResult extractionResult
) {
}
