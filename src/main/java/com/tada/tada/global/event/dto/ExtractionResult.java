package com.tada.tada.global.event.dto;

import java.util.List;

public record ExtractionResult(
        List<PersonExtraction> persons,
        List<PlaceExtraction> places,
        List<ActivityExtraction> activities
) {
}
