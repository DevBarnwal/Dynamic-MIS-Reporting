package com.example.mis.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public record ReportFilterRequest(Map<String, Object> filters) {
    public Map<String, Object> safeFilters() {
        return filters == null ? new LinkedHashMap<>() : new LinkedHashMap<>(filters);
    }
}
