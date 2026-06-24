package com.example.mis.service;

import com.example.mis.dto.ReportResult;
import com.example.mis.dto.CurrentUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReportExportService {
    private final AuditService auditService;

    public ReportExportService(AuditService auditService) {
        this.auditService = auditService;
    }

    public ExportFile export(ReportResult result, String format, CurrentUser user) {
        if (!user.canExport()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Export is not allowed for this role");
        }
        auditService.log(user, "EXPORT_" + format.toUpperCase(), result.reportId(), result.totalRows() + " rows");
        return switch (format.toLowerCase()) {
            case "xlsx" -> new ExportFile(
                    filename(result, "xlsx"),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    toXlsx(result)
            );
            case "pdf" -> new ExportFile(filename(result, "pdf"), "application/pdf", toPdf(result));
            case "jpg", "jpeg" -> new ExportFile(filename(result, "jpg"), "image/jpeg", toJpg(result));
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported export format");
        };
    }

    private byte[] toXlsx(ReportResult result) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Report");
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            List<Column> columns = columns(result.outputColumns());
            for (int i = 0; i < columns.size(); i++) {
                var cell = header.createCell(i);
                cell.setCellValue(columns.get(i).label());
                cell.setCellStyle(headerStyle);
            }

            for (int rowIndex = 0; rowIndex < result.rows().size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                Map<String, Object> data = result.rows().get(rowIndex);
                for (int colIndex = 0; colIndex < columns.size(); colIndex++) {
                    Object value = data.get(columns.get(colIndex).column());
                    row.createCell(colIndex).setCellValue(value == null ? "" : value.toString());
                }
            }

            for (int i = 0; i < columns.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not export XLSX");
        }
    }

    private byte[] toPdf(ReportResult result) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            List<Column> columns = columns(result.outputColumns());
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new com.lowagie.text.Paragraph(result.reportName()));
            document.add(new com.lowagie.text.Paragraph("Total rows: " + result.totalRows()));
            document.add(new com.lowagie.text.Paragraph(" "));

            PdfPTable table = new PdfPTable(columns.size());
            table.setWidthPercentage(100);
            for (Column column : columns) {
                PdfPCell cell = new PdfPCell(new Phrase(column.label()));
                table.addCell(cell);
            }
            for (Map<String, Object> row : result.rows()) {
                for (Column column : columns) {
                    Object value = row.get(column.column());
                    table.addCell(value == null ? "" : value.toString());
                }
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not export PDF");
        }
    }

    private byte[] toJpg(ReportResult result) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            List<Column> columns = columns(result.outputColumns());
            int cellWidth = 170;
            int rowHeight = 34;
            int titleHeight = 82;
            int width = Math.max(900, columns.size() * cellWidth + 40);
            int height = Math.max(240, titleHeight + (result.rows().size() + 1) * rowHeight + 40);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setColor(new Color(24, 40, 64));
            g.setFont(new Font("SansSerif", Font.BOLD, 24));
            g.drawString(result.reportName(), 20, 35);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.drawString("Total rows: " + result.totalRows(), 20, 60);

            int x = 20;
            int y = titleHeight;
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(new Color(0, 97, 168));
            g.fillRect(x, y, columns.size() * cellWidth, rowHeight);
            g.setColor(Color.WHITE);
            for (int i = 0; i < columns.size(); i++) {
                g.drawString(columns.get(i).label(), x + i * cellWidth + 8, y + 22);
            }

            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            for (int rowIndex = 0; rowIndex < result.rows().size(); rowIndex++) {
                y += rowHeight;
                g.setColor(rowIndex % 2 == 0 ? new Color(248, 250, 252) : Color.WHITE);
                g.fillRect(x, y, columns.size() * cellWidth, rowHeight);
                g.setColor(new Color(34, 45, 59));
                Map<String, Object> row = result.rows().get(rowIndex);
                for (int colIndex = 0; colIndex < columns.size(); colIndex++) {
                    Object value = row.get(columns.get(colIndex).column());
                    g.drawString(truncate(value == null ? "" : value.toString(), 22), x + colIndex * cellWidth + 8, y + 22);
                }
            }

            g.dispose();
            ImageIO.write(image, "jpg", out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not export JPG");
        }
    }

    private List<Column> columns(JsonNode outputColumns) {
        List<Column> columns = new ArrayList<>();
        for (JsonNode column : outputColumns) {
            columns.add(new Column(column.path("column").asText(), column.path("label").asText()));
        }
        return columns;
    }

    private String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }

    private String filename(ReportResult result, String extension) {
        return result.reportName().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "") + "." + extension;
    }

    public record ExportFile(String filename, String contentType, byte[] bytes) {
    }

    private record Column(String column, String label) {
    }
}
