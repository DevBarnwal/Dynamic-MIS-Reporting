package com.example.mis.dto;

public record CurrentUser(
        Long userId,
        String username,
        String fullName,
        String roleCode,
        Long departmentId,
        Long courseId,
        Long studentId
) {
    public boolean isAdmin() {
        return "ADMIN".equals(roleCode);
    }

    public boolean isViewer() {
        return "REPORT_VIEWER".equals(roleCode);
    }

    public boolean canExport() {
        return isAdmin() || isViewer() || "HOD".equals(roleCode()) || "FACULTY".equals(roleCode()) || "STUDENT".equals(roleCode());
    }
}
