package org.nep.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nep.common.result.PageResult;
import org.nep.common.result.Result;
import org.nep.system.entity.Knowledge;
import org.nep.system.service.KnowledgeService;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 环保知识库控制器
 * 提供环保知识文章的分类浏览、搜索、点赞等功能
 */
@Tag(name = "环保知识库")
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    public KnowledgeController(KnowledgeService knowledgeService) { this.knowledgeService = knowledgeService; }

    @Operation(summary = "分页查询知识库")
    @GetMapping("/page")
    public PageResult<java.util.List<Knowledge>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Knowledge> w = new LambdaQueryWrapper<>();
        w.eq(Knowledge::getStatus, 1);
        if (category != null && !category.isEmpty()) w.eq(Knowledge::getCategory, category);
        if (keyword != null && !keyword.isEmpty()) w.like(Knowledge::getTitle, keyword);
        w.orderByDesc(Knowledge::getCreateTime);
        Page<Knowledge> r = knowledgeService.page(new Page<>(page, size), w);
        return PageResult.ok(r.getRecords(), r.getCurrent(), r.getSize(), r.getTotal());
    }

    @Operation(summary = "获取知识详情")
    @GetMapping("/{id}")
    public Result<Knowledge> getById(@PathVariable Long id) {
        knowledgeService.incrementViewCount(id);
        return Result.ok(knowledgeService.getById(id));
    }

    @Operation(summary = "热门知识推荐")
    @GetMapping("/hot")
    public Result<java.util.List<Knowledge>> hot(@RequestParam(defaultValue = "5") Integer count) {
        LambdaQueryWrapper<Knowledge> w = new LambdaQueryWrapper<>();
        w.eq(Knowledge::getStatus, 1).orderByDesc(Knowledge::getViewCount).last("LIMIT " + count);
        return Result.ok(knowledgeService.list(w));
    }

    @Operation(summary = "点赞知识")
    @PostMapping("/like/{id}")
    public Result<Void> like(@PathVariable Long id) {
        knowledgeService.incrementLikeCount(id);
        return Result.success("点赞成功");
    }

    @Operation(summary = "发布知识文章（管理员）")
    @PostMapping
    public Result<Knowledge> create(@RequestBody Knowledge knowledge) {
        knowledge.setStatus(1);
        knowledge.setViewCount(0L);
        knowledge.setLikeCount(0L);
        knowledgeService.save(knowledge);
        return Result.ok("发布成功", knowledge);
    }

    @Operation(summary = "更新知识文章（管理员）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Knowledge knowledge) {
        knowledge.setId(id);
        knowledgeService.updateById(knowledge);
        return Result.success("更新成功");
    }

    @Operation(summary = "删除知识文章（管理员）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeService.removeById(id);
        return Result.success("删除成功");
    }

    /** 系统内置中文字体路径（Windows） */
    private static final String FONT_PATH = "C:/Windows/Fonts/simsun.ttc,0";

    @Operation(summary = "导出知识库内容为PDF文档")
    @GetMapping("/{id}/export")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> exportContent(@PathVariable Long id) {
        Knowledge doc = knowledgeService.getById(id);
        if (doc == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }

        byte[] pdfBytes = buildPdf(doc);
        org.springframework.core.io.ByteArrayResource resource =
                new org.springframework.core.io.ByteArrayResource(pdfBytes);

        String filename = (doc.getTitle() != null ? doc.getTitle() : "knowledge") + ".pdf";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(resource);
    }

    /** 使用 iText7 生成完整 PDF 文档 */
    private byte[] buildPdf(Knowledge doc) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // 加载中文字体
            PdfFont titleFont = PdfFontFactory.createFont(FONT_PATH, PdfEncodings.IDENTITY_H);
            PdfFont headingFont = PdfFontFactory.createFont(FONT_PATH, PdfEncodings.IDENTITY_H);
            PdfFont bodyFont = PdfFontFactory.createFont(FONT_PATH, PdfEncodings.IDENTITY_H);
            PdfFont metaFont = PdfFontFactory.createFont(FONT_PATH, PdfEncodings.IDENTITY_H);

            String title = doc.getTitle() != null ? doc.getTitle() : "环保知识文献";
            String category = switch (doc.getCategory() != null ? doc.getCategory() : "") {
                case "AIR" -> "大气环境"; case "WATER" -> "水环境";
                case "SOIL" -> "土壤环境"; case "NOISE" -> "噪声污染";
                case "ECOLOGY" -> "生态保护"; default -> "综合";
            };
            String source = doc.getSource() != null ? doc.getSource() : "东软环保公众监督系统";
            String summary = doc.getSummary() != null ? doc.getSummary() : "暂无摘要";
            String bodyText = stripHtml(doc.getContent());

            // === 标题 ===
            document.add(new Paragraph()
                    .add(new Text(title).setFont(titleFont).setFontSize(20f))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(8));

            // === 元信息 ===
            String meta = "分类：" + category + "  ｜  来源：" + source + "  ｜  导出时间："
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            document.add(new Paragraph()
                    .add(new Text(meta).setFont(metaFont).setFontSize(9f).setFontColor(
                            new com.itextpdf.kernel.colors.DeviceRgb(0x74, 0x80, 0x7B)))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(24));

            // === 分割线 ===
            document.add(new Paragraph()
                    .setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(0.8f))
                    .setMarginBottom(20));

            // === 内容提要 ===
            document.add(new Paragraph()
                    .add(new Text("【内容提要】").setFont(headingFont).setFontSize(12f))
                    .setMarginBottom(6));
            document.add(new Paragraph()
                    .add(new Text(summary).setFont(bodyFont).setFontSize(10.5f))
                    .setMarginBottom(20));

            // === 正文 ===
            document.add(new Paragraph()
                    .add(new Text("【正文】").setFont(headingFont).setFontSize(12f))
                    .setMarginBottom(10));
            document.add(new Paragraph()
                    .add(new Text(bodyText).setFont(bodyFont).setFontSize(10.5f))
                    .setFixedLeading(22f)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.JUSTIFIED)
                    .setMarginBottom(30));

            // === 页脚 ===
            document.add(new Paragraph()
                    .setBorderTop(new com.itextpdf.layout.borders.SolidBorder(0.4f))
                    .setMarginTop(20));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF生成失败: " + e.getMessage(), e);
        }
    }

    /** 去除 HTML 标签，保留纯文本 */
    private String stripHtml(String html) {
        if (html == null || html.isEmpty()) return "暂无正文内容";
        return html
                .replaceAll("<br\\s*/?>", "\n")
                .replaceAll("</p>", "\n")
                .replaceAll("</h[1-6]>", "\n")
                .replaceAll("</li>", "\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("&quot;", "\"")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }
}
