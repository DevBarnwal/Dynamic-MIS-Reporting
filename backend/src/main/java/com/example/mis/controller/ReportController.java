package com.example.mis.controller;

import com.example.mis.dto.ReportDefinition;
import com.example.mis.dto.ReportFilterRequest;
import com.example.mis.dto.ReportResult;
import com.example.mis.service.AuthService;
import com.example.mis.service.ReportExportService;
import com.example.mis.service.ReportService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;
    private final ReportExportService exportService;
    private final AuthService authService;

    public ReportController(ReportService reportService, ReportExportService exportService, AuthService authService) {
        this.reportService = reportService;
        this.exportService = exportService;
        this.authService = authService;
    }

    @GetMapping
    public List<ReportDefinition> listReports(@org.springframework.web.bind.annotation.RequestHeader("Authorization") String authorization) {
        return reportService.listReports(authService.requireUser(authorization));
    }

    @GetMapping("/{reportId}")
    public ReportDefinition getReport(@PathVariable long reportId, @org.springframework.web.bind.annotation.RequestHeader("Authorization") String authorization) {
        return reportService.getReportDefinition(reportId, authService.requireUser(authorization));
    }

    @GetMapping("/{reportId}/options/{filterName}")
    public List<Map<String, Object>> getDropdownOptions(
            @PathVariable long reportId,
            @PathVariable String filterName,
            @org.springframework.web.bind.annotation.RequestHeader("Authorization") String authorization
    ) {
        return reportService.getDropdownOptions(reportId, filterName, authService.requireUser(authorization));
    }

    @PostMapping("/{reportId}/run")
    public ReportResult runReport(
            @PathVariable long reportId,
            @RequestBody ReportFilterRequest request,
            @org.springframework.web.bind.annotation.RequestHeader("Authorization") String authorization
    ) {
        return reportService.runReport(reportId, request.safeFilters(), authService.requireUser(authorization));
    }

    @PostMapping("/{reportId}/export/{format}")
    public ResponseEntity<byte[]> exportReport(
            @PathVariable long reportId,
            @PathVariable String format,
            @RequestBody ReportFilterRequest request,
            @org.springframework.web.bind.annotation.RequestHeader("Authorization") String authorization
    ) {
        var user = authService.requireUser(authorization);
        ReportResult result = reportService.runReport(reportId, request.safeFilters(), user);
        ReportExportService.ExportFile file = exportService.export(result, format, user);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.filename())
                        .build()
                        .toString())
                .body(file.bytes());
    }
}
