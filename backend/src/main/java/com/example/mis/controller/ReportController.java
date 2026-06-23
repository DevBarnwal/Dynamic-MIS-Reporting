package com.example.mis.controller;

import com.example.mis.dto.ReportDefinition;
import com.example.mis.dto.ReportFilterRequest;
import com.example.mis.dto.ReportResult;
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

    public ReportController(ReportService reportService, ReportExportService exportService) {
        this.reportService = reportService;
        this.exportService = exportService;
    }

    @GetMapping
    public List<ReportDefinition> listReports() {
        return reportService.listReports();
    }

    @GetMapping("/{reportId}")
    public ReportDefinition getReport(@PathVariable long reportId) {
        return reportService.getReportDefinition(reportId);
    }

    @GetMapping("/{reportId}/options/{filterName}")
    public List<Map<String, Object>> getDropdownOptions(
            @PathVariable long reportId,
            @PathVariable String filterName
    ) {
        return reportService.getDropdownOptions(reportId, filterName);
    }

    @PostMapping("/{reportId}/run")
    public ReportResult runReport(@PathVariable long reportId, @RequestBody ReportFilterRequest request) {
        return reportService.runReport(reportId, request.safeFilters());
    }

    @PostMapping("/{reportId}/export/{format}")
    public ResponseEntity<byte[]> exportReport(
            @PathVariable long reportId,
            @PathVariable String format,
            @RequestBody ReportFilterRequest request
    ) {
        ReportResult result = reportService.runReport(reportId, request.safeFilters());
        ReportExportService.ExportFile file = exportService.export(result, format);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.filename())
                        .build()
                        .toString())
                .body(file.bytes());
    }
}
