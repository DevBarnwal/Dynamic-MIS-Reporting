package com.example.mis.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record ReportDefinition(
        Long reportId,
        String reportName,
        JsonNode inputFilters,
        JsonNode outputColumns,
        LocalDateTime createdAt
) {
}
