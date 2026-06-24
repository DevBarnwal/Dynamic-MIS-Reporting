package com.example.mis.service;

import com.example.mis.dto.ReportDefinition;
import com.example.mis.dto.ReportResult;
import com.example.mis.dto.CurrentUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class ReportService {
    private static final Pattern NAMED_PARAM = Pattern.compile(":([a-zA-Z][a-zA-Z0-9_]*)");

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public ReportService(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper, AuditService auditService) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    public List<ReportDefinition> listReports(CurrentUser user) {
        return jdbc.query("""
                SELECT report_id, report_name, input_filters, output_columns, created_at
                FROM dynamic_report
                WHERE :canManage = TRUE OR report_name <> 'Audit Log'
                ORDER BY report_id
                """, Map.of("canManage", canManageReports(user)), (rs, rowNum) -> new ReportDefinition(
                rs.getLong("report_id"),
                rs.getString("report_name"),
                readJson(rs.getString("input_filters")),
                readJson(rs.getString("output_columns")),
                rs.getTimestamp("created_at").toLocalDateTime()
        ));
    }

    public ReportDefinition getReportDefinition(long reportId, CurrentUser user) {
        ReportRecord record = getReportRecord(reportId);
        ensureReportAllowed(record, user);
        return new ReportDefinition(
                record.reportId(),
                record.reportName(),
                record.inputFilters(),
                record.outputColumns(),
                record.createdAt().toLocalDateTime()
        );
    }

    public List<Map<String, Object>> getDropdownOptions(long reportId, String filterName, CurrentUser user) {
        ReportRecord record = getReportRecord(reportId);
        ensureReportAllowed(record, user);
        JsonNode filter = findFilter(record.inputFilters(), filterName);

        if (filter == null || !"dropdown".equals(filter.path("type").asText())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dropdown filter not found");
        }

        String dropdownQuery = filter.path("dropdown_query").asText(null);
        if (dropdownQuery == null || !dropdownQuery.trim().toLowerCase(Locale.ROOT).startsWith("select")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dropdown query is not configured");
        }

        return jdbc.queryForList(dropdownQuery, Map.of()).stream()
                .filter(option -> dropdownOptionAllowed(filterName, option, user))
                .toList();
    }

    public ReportResult runReport(long reportId, Map<String, Object> rawFilters, CurrentUser user) {
        ReportRecord record = getReportRecord(reportId);
        ensureReportAllowed(record, user);
        Map<String, Object> filters = normalizeFilters(record.inputFilters(), rawFilters);
        applyRoleScope(filters, user);
        ensureAllQueryParamsBound(record.query(), filters);

        List<Map<String, Object>> rows = jdbc.queryForList(record.query(), filters).stream()
                .map(this::normalizeRow)
                .toList();
        auditService.log(user, "RUN_REPORT", record.reportId(), record.reportName() + " returned " + rows.size() + " rows");

        return new ReportResult(
                record.reportId(),
                record.reportName(),
                record.outputColumns(),
                rows,
                rows.size()
        );
    }

    private ReportRecord getReportRecord(long reportId) {
        try {
            return jdbc.queryForObject("""
                    SELECT report_id, report_name, input_filters, output_columns, query, created_at
                    FROM dynamic_report
                    WHERE report_id = :reportId
                    """, Map.of("reportId", reportId), (rs, rowNum) -> new ReportRecord(
                    rs.getLong("report_id"),
                    rs.getString("report_name"),
                    readJson(rs.getString("input_filters")),
                    readJson(rs.getString("output_columns")),
                    rs.getString("query"),
                    rs.getTimestamp("created_at")
            ));
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
        }
    }

    private Map<String, Object> normalizeFilters(JsonNode inputFilters, Map<String, Object> rawFilters) {
        Map<String, Object> normalized = new HashMap<>();

        for (JsonNode filter : inputFilters) {
            String name = filter.path("name").asText();
            String type = filter.path("type").asText();
            Object raw = rawFilters.get(name);

            if (raw == null || raw.toString().isBlank()) {
                normalized.put(name, null);
                continue;
            }

            normalized.put(name, switch (type) {
                case "date" -> LocalDate.parse(raw.toString());
                case "dropdown" -> Long.valueOf(raw.toString());
                case "number" -> new BigDecimal(raw.toString());
                default -> raw.toString();
            });
        }

        return normalized;
    }

    private void applyRoleScope(Map<String, Object> filters, CurrentUser user) {
        switch (user.roleCode()) {
            case "HOD" -> filters.put("department_id", user.departmentId());
            case "FACULTY" -> filters.put("course_id", user.courseId());
            case "STUDENT" -> {
                filters.put("student_id", user.studentId());
                filters.put("student_roll_no", getStudentRollNo(user.studentId()));
            }
            default -> {
            }
        }
    }

    private boolean dropdownOptionAllowed(String filterName, Map<String, Object> option, CurrentUser user) {
        Object value = option.get("value");
        if ("department_id".equals(filterName) && "HOD".equals(user.roleCode())) {
            return value != null && user.departmentId() != null && value.toString().equals(user.departmentId().toString());
        }
        if ("course_id".equals(filterName) && "FACULTY".equals(user.roleCode())) {
            return value != null && user.courseId() != null && value.toString().equals(user.courseId().toString());
        }
        return true;
    }

    private void ensureReportAllowed(ReportRecord record, CurrentUser user) {
        if ("Audit Log".equals(record.reportName()) && !canManageReports(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Report is not available for this role");
        }
    }

    private boolean canManageReports(CurrentUser user) {
        return user.isAdmin();
    }

    private String getStudentRollNo(Long studentId) {
        if (studentId == null) {
            return null;
        }
        try {
            return jdbc.queryForObject(
                    "SELECT student_roll_no FROM student WHERE student_id = :studentId",
                    Map.of("studentId", studentId),
                    String.class
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private void ensureAllQueryParamsBound(String sql, Map<String, Object> filters) {
        Matcher matcher = NAMED_PARAM.matcher(sql);
        while (matcher.find()) {
            String param = matcher.group(1);
            if (!filters.containsKey(param)) {
                filters.put(param, null);
            }
        }
    }

    private JsonNode findFilter(JsonNode inputFilters, String filterName) {
        for (JsonNode filter : inputFilters) {
            if (filterName.equals(filter.path("name").asText())) {
                return filter;
            }
        }
        return null;
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid report JSON");
        }
    }

    private Map<String, Object> normalizeRow(Map<String, Object> row) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        row.forEach((key, value) -> normalized.put(key.toLowerCase(Locale.ROOT), value));
        return normalized;
    }

    private record ReportRecord(
            Long reportId,
            String reportName,
            JsonNode inputFilters,
            JsonNode outputColumns,
            String query,
            Timestamp createdAt
    ) {
    }
}
