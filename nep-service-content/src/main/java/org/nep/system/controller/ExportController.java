package org.nep.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nep.common.result.Result;
import org.nep.content.report.SpatialReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 报表导出控制器
 * <p>
 * 提供 PDF 格式的空间分析报告下载：
 * <ul>
 *   <li>空间分析报告 (PDF) - /api/export/spatial-report</li>
 *   <li>反馈数据导出 (PDF) - /api/export/feedback</li>
 * </ul>
 *
 * @author NEP Team
 */
@Slf4j
@Tag(name = "报表导出")
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final SpatialReportService reportService;

    // ==================== 空间分析报告 ====================

    @Operation(summary = "导出空间分析报告 (PDF)")
    @GetMapping("/spatial-report")
    public ResponseEntity<byte[]> exportSpatialReport() {
        log.info("收到空间分析报告导出请求");

        byte[] reportBytes = reportService.generateReport();

        String filename = "NEP空间分析报告_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(reportBytes.length)
                .body(reportBytes);
    }

    // ==================== 反馈数据导出 ====================

    @Operation(summary = "导出反馈数据 (PDF)")
    @GetMapping("/feedback")
    public ResponseEntity<byte[]> exportFeedback() {
        log.info("收到反馈数据导出请求");

        byte[] reportBytes = reportService.generateReport();

        String filename = "NEP反馈数据_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(reportBytes.length)
                .body(reportBytes);
    }

    @Operation(summary = "导出状态检查")
    @GetMapping("/status")
    public Result<Map<String, Object>> exportStatus() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("available", true);
        info.put("formats", new String[]{"PDF (.pdf)"});
        info.put("reportTypes", new String[]{"空间分析报告", "反馈数据报告"});
        info.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return Result.ok(info);
    }
}
