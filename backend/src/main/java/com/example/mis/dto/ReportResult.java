package com.example.mis.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

public record ReportResult(
        Long reportId,
        String reportName,
        JsonNode outputColumns,
        List<Map<String, Object>> rows,
        int totalRows
) {
}
