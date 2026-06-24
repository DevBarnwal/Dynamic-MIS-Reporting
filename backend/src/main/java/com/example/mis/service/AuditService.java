package com.example.mis.service;

import com.example.mis.dto.CurrentUser;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final NamedParameterJdbcTemplate jdbc;

    public AuditService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void log(CurrentUser user, String action, Long reportId, String details) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("userId", user.userId());
        params.put("action", action);
        params.put("reportId", reportId);
        params.put("details", details == null ? "" : details);

        jdbc.update("""
                INSERT INTO audit_log (user_id, action, report_id, details)
                VALUES (:userId, :action, :reportId, :details)
                """, params);
    }
}
