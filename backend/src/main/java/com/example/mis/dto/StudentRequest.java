package com.example.mis.dto;

import java.math.BigDecimal;

public record StudentRequest(
        String studentRollNo,
        String studentName,
        Long departmentId,
        Long courseId,
        Integer semester,
        BigDecimal marks,
        BigDecimal attendancePercentage
) {}
