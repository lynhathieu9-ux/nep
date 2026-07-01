package org.nep.content.report;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nep.system.dto.MapAqiResult;
import org.nep.system.entity.AqiDetection;
import org.nep.system.mapper.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 空间分析报告生成服务 (PDF格式)
 * <p>
 * 生成包含以下章节的 PDF 报告：
 * <ol>
 *   <li>封面 + 宏观态势总览 - KPI 指标卡片</li>
 *   <li>污染热点地图 - 省份/城市 AQI 聚合</li>
 *   <li>AQI 等级分布 - 六等级统计</li>
 *   <li>反馈状态分布 - 工单状态</li>
 *   <li>各省份统计 - 按省份排名</li>
 *   <li>月度趋势 - 近12月变化</li>
 * </ol>
 *
 * @author NEP Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpatialReportService {

    private final AqiDetectionMapper aqiMapper;
    private final ProvinceMapper provinceMapper;
    private final CityMapper cityMapper;
    private final SupervisionFeedbackMapper feedbackMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // NEP 品牌色
    private static final DeviceRgb COLOR_PRIMARY = new DeviceRgb(24, 144, 255);    // #1890FF
    private static final DeviceRgb COLOR_DARK = new DeviceRgb(0, 42, 85);          // 深蓝
    private static final DeviceRgb COLOR_HEADER_BG = new DeviceRgb(0, 42, 85);
    private static final DeviceRgb COLOR_STRIPE = new DeviceRgb(240, 247, 255);
    private static final DeviceRgb COLOR_TEXT = new DeviceRgb(51, 51, 51);
    private static final DeviceRgb COLOR_GRAY = new DeviceRgb(140, 140, 140);

    // AQI 等级颜色
    private static final DeviceRgb[] AQI_COLORS = {
        new DeviceRgb(0, 228, 0),       // 优 - 绿色
        new DeviceRgb(255, 255, 0),     // 良 - 黄色
        new DeviceRgb(255, 126, 0),     // 轻度 - 橙色
        new DeviceRgb(255, 0, 0),       // 中度 - 红色
        new DeviceRgb(153, 0, 76),      // 重度 - 紫色
        new DeviceRgb(126, 0, 35)       // 严重 - 褐红
    };

    /**
     * 生成空间分析报告 PDF 文件
     * @return byte[] PDF 文件字节数组
     */
    public byte[] generateReport() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(45, 40, 45, 40);

            // 加载中文字体
            PdfFont titleFont = loadFont("STSong-Light", "UniGB-UCS2-H");
            PdfFont bodyFont = titleFont; // 复用

            // 页脚：页码
            pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new IEventHandler() {
                @Override
                public void handleEvent(Event event) {
                    try {
                        PdfDocumentEvent e = (PdfDocumentEvent) event;
                        PdfCanvas canvas = new PdfCanvas(e.getPage());
                        canvas.beginText()
                            .setFontAndSize(PdfFontFactory.createFont(), 9)
                            .moveText(40, 25)
                            .showText("- " + pdf.getPageNumber(e.getPage()) + " -")
                            .endText();
                    } catch (java.io.IOException ignored) {
                        // 页脚渲染失败不影响主体内容
                    }
                }
            });

            // ===== 封面 + 宏观态势总览 =====
            renderCoverAndOverview(doc, pdf, titleFont, bodyFont);

            // ===== 污染热点地图 =====
            doc.add(new AreaBreak());
            renderHotspotMap(doc, titleFont, bodyFont);

            // ===== AQI 等级分布 =====
            doc.add(new AreaBreak());
            renderAqiDistribution(doc, titleFont, bodyFont);

            // ===== 反馈状态分布 =====
            doc.add(new AreaBreak());
            renderFeedbackStatus(doc, titleFont, bodyFont);

            // ===== 各省份统计 =====
            doc.add(new AreaBreak());
            renderProvinceStats(doc, titleFont, bodyFont);

            // ===== 月度趋势 =====
            doc.add(new AreaBreak());
            renderMonthlyTrend(doc, titleFont, bodyFont);

            int pageCount = pdf.getNumberOfPages();
            doc.close();
            log.info("PDF空间分析报告生成成功，文件大小: {} bytes，共 {} 页",
                    baos.size(), pageCount);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("生成PDF报告失败", e);
            throw new RuntimeException("报告生成失败: " + e.getMessage(), e);
        }
    }

    // ==================== 封面 + 宏观态势总览 ====================

    private void renderCoverAndOverview(Document doc, PdfDocument pdf, PdfFont titleFont, PdfFont bodyFont) {
        // 报告标题
        Paragraph title = new Paragraph("东软环保公众监督系统")
                .setFont(titleFont).setFontSize(24).setFontColor(COLOR_DARK)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(80);
        doc.add(title);

        Paragraph subtitle = new Paragraph("空间分析报告")
                .setFont(titleFont).setFontSize(18).setFontColor(COLOR_PRIMARY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(12);
        doc.add(subtitle);

        Paragraph time = new Paragraph("生成时间: " + LocalDateTime.now().format(DT_FMT))
                .setFont(bodyFont).setFontSize(10).setFontColor(COLOR_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(40);
        doc.add(time);

        // 分隔线
        doc.add(new Divider().setMarginBottom(24));

        // KPI 数据
        long totalDetections = aqiMapper.selectCount(null);
        long totalProvinces = provinceMapper.selectCount(null);
        long totalCities = cityMapper.selectCount(null);
        long totalFeedbacks = 0, pendingFeedbacks = 0, completedFeedbacks = 0;
        try {
            totalFeedbacks = feedbackMapper.selectCount(null);
            pendingFeedbacks = feedbackMapper.countPendingFeedback();
            completedFeedbacks = feedbackMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                            org.nep.system.entity.SupervisionFeedback>()
                            .eq(org.nep.system.entity.SupervisionFeedback::getStatus, "COMPLETED"));
        } catch (Exception ignored) {}

        // KPI 表格
        float[] colWidths = {200, 120, 230};
        Table kpiTable = new Table(UnitValue.createPercentArray(colWidths))
                .useAllAvailableWidth().setMarginTop(16);

        addKpiRow(kpiTable, bodyFont, "AQI 检测总数", String.valueOf(totalDetections), "次");
        addKpiRow(kpiTable, bodyFont, "监督反馈总数", String.valueOf(totalFeedbacks), "条");
        addKpiRow(kpiTable, bodyFont, "覆盖省份数", String.valueOf(totalProvinces), "个");
        addKpiRow(kpiTable, bodyFont, "覆盖城市数", String.valueOf(totalCities), "个");
        addKpiRow(kpiTable, bodyFont, "已完成反馈", String.valueOf(completedFeedbacks), "条");
        addKpiRow(kpiTable, bodyFont, "待处理反馈", String.valueOf(pendingFeedbacks), "条");
        addKpiRow(kpiTable, bodyFont, "城市覆盖率",
                totalCities > 0 ? Math.round((double) totalCities / 339 * 100) + "%" : "0%", "");

        doc.add(kpiTable);
    }

    // ==================== 污染热点地图 ====================

    private void renderHotspotMap(Document doc, PdfFont titleFont, PdfFont bodyFont) {
        addSectionHeader(doc, titleFont, bodyFont, "污染热点地图", "全国城市AQI分布（近30天）");

        List<Map<String, Object>> cityData = aqiMapper.aggregateByCity30Days();
        if (cityData.isEmpty()) {
            doc.add(new Paragraph("暂无数据").setFont(bodyFont).setFontSize(10).setFontColor(COLOR_GRAY)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(20));
            return;
        }

        float[] colWidths = {90, 80, 75, 75, 70, 65, 80};
        Table table = createTable(colWidths);
        String[] headers = {"城市", "省份", "平均AQI", "最大AQI", "检测次数", "等级", "等级标签"};
        addTableHeader(table, bodyFont, headers);

        int idx = 0;
        for (Map<String, Object> row : cityData) {
            double avgAqi = toDouble(row.get("avgAqi"));
            int level = MapAqiResult.aqiToLevel(avgAqi);
            String[] cells = {
                toString(row.get("cityName")),
                toString(row.get("provinceName")),
                formatDouble(avgAqi),
                String.valueOf(toInt(row.get("maxAqi"))),
                String.valueOf(toLong(row.get("detectionCount"))),
                String.valueOf(level),
                MapAqiResult.levelToLabel(level)
            };
            addDataRow(table, bodyFont, cells, idx++);
        }
        doc.add(table);
    }

    // ==================== AQI 等级分布 ====================

    private void renderAqiDistribution(Document doc, PdfFont titleFont, PdfFont bodyFont) {
        addSectionHeader(doc, titleFont, bodyFont, "AQI等级分布", "按国家AQI标准六等级划分");

        String[] names = {"优", "良", "轻度污染", "中度污染", "重度污染", "严重污染"};
        int[] rangesLow = {0, 51, 101, 151, 201, 301};
        int[] rangesHigh = {50, 100, 150, 200, 300, Integer.MAX_VALUE};
        long total = aqiMapper.selectCount(null);

        float[] colWidths = {50, 100, 100, 100, 80};
        Table table = createTable(colWidths);
        String[] headers = {"等级", "名称", "AQI范围", "检测次数", "占比"};
        addTableHeader(table, bodyFont, headers);

        for (int i = 0; i < 6; i++) {
            long count;
            if (i == 5) {
                count = aqiMapper.selectCount(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AqiDetection>()
                                .gt(AqiDetection::getFinalAqi, 300));
            } else {
                count = aqiMapper.selectCount(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AqiDetection>()
                                .ge(AqiDetection::getFinalAqi, rangesLow[i])
                                .le(AqiDetection::getFinalAqi, rangesHigh[i]));
            }
            String[] cells = {
                String.valueOf(i + 1),
                names[i],
                rangesLow[i] + (i < 5 ? "-" + rangesHigh[i] : "+"),
                String.valueOf(count),
                total > 0 ? Math.round((double) count / total * 100) + "%" : "0%"
            };
            addDataRow(table, bodyFont, cells, i, AQI_COLORS[i]);
        }
        doc.add(table);
    }

    // ==================== 反馈状态分布 ====================

    private void renderFeedbackStatus(Document doc, PdfFont titleFont, PdfFont bodyFont) {
        addSectionHeader(doc, titleFont, bodyFont, "反馈状态分布", "监督反馈工单各状态统计");

        String[][] statuses = {
            {"PENDING", "待指派"},
            {"ASSIGNED", "已指派/处理中"},
            {"COMPLETED", "已完成"},
            {"ESCALATED", "已升级/催办"},
            {"REJECTED", "已拒绝"}
        };

        float[] colWidths = {120, 200, 100};
        Table table = createTable(colWidths);
        String[] headers = {"状态编码", "说明", "数量"};
        addTableHeader(table, bodyFont, headers);

        int idx = 0;
        try {
            for (String[] s : statuses) {
                long count = feedbackMapper.selectCount(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                                org.nep.system.entity.SupervisionFeedback>()
                                .eq(org.nep.system.entity.SupervisionFeedback::getStatus, s[0]));
                String[] cells = {s[0], s[1], String.valueOf(count)};
                addDataRow(table, bodyFont, cells, idx++);
            }
        } catch (Exception e) {
            doc.add(new Paragraph("反馈数据暂不可用").setFont(bodyFont).setFontSize(10)
                    .setFontColor(COLOR_GRAY).setMarginTop(12));
            return;
        }
        doc.add(table);
    }

    // ==================== 各省份统计 ====================

    private void renderProvinceStats(Document doc, PdfFont titleFont, PdfFont bodyFont) {
        addSectionHeader(doc, titleFont, bodyFont, "各省份统计", "各省份AQI与检测统计（近30天，按平均AQI降序）");

        List<Map<String, Object>> rows = aqiMapper.aggregateByProvince30Days();
        if (rows.isEmpty()) {
            doc.add(new Paragraph("暂无数据").setFont(bodyFont).setFontSize(10).setFontColor(COLOR_GRAY)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(20));
            return;
        }

        float[] colWidths = {40, 80, 75, 75, 75, 70, 80};
        Table table = createTable(colWidths);
        String[] headers = {"#", "省份", "平均AQI", "最大AQI", "检测次数", "覆盖城市", "等级"};
        addTableHeader(table, bodyFont, headers);

        int rank = 1, idx = 0;
        for (Map<String, Object> row : rows) {
            double avgAqi = toDouble(row.get("avgAqi"));
            String[] cells = {
                String.valueOf(rank++),
                toString(row.get("provinceName")),
                formatDouble(avgAqi),
                String.valueOf(toInt(row.get("maxAqi"))),
                String.valueOf(toLong(row.get("totalDetections"))),
                String.valueOf(toInt(row.get("cityCount"))),
                MapAqiResult.levelToLabel(MapAqiResult.aqiToLevel(avgAqi))
            };
            addDataRow(table, bodyFont, cells, idx++);
        }
        doc.add(table);
    }

    // ==================== 月度趋势 ====================

    private void renderMonthlyTrend(Document doc, PdfFont titleFont, PdfFont bodyFont) {
        addSectionHeader(doc, titleFont, bodyFont, "月度趋势", "AQI检测月度统计（近12个月）");

        float[] colWidths = {180, 160, 160};
        Table table = createTable(colWidths);
        String[] headers = {"月份", "检测次数", "环比变化"};
        addTableHeader(table, bodyFont, headers);

        LocalDateTime now = LocalDateTime.now();
        long prevCount = -1;
        for (int i = 11; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1);
            long count = aqiMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AqiDetection>()
                            .ge(AqiDetection::getDetectionTime, monthStart.format(DT_FMT))
                            .lt(AqiDetection::getDetectionTime, monthEnd.format(DT_FMT)));

            String change;
            if (prevCount >= 0 && prevCount > 0) {
                change = Math.round((double)(count - prevCount) / prevCount * 100) + "%";
            } else {
                change = "-";
            }
            String[] cells = {
                monthStart.getYear() + "-" + String.format("%02d", monthStart.getMonthValue()),
                String.valueOf(count),
                change
            };
            addDataRow(table, bodyFont, cells, i); // i as index for stripe
            prevCount = count;
        }
        doc.add(table);
    }

    // ==================== 样式工具方法 ====================

    /** 章节标题 */
    private void addSectionHeader(Document doc, PdfFont titleFont, PdfFont bodyFont,
                                   String title, String desc) {
        Paragraph t = new Paragraph(title)
                .setFont(titleFont).setFontSize(16).setFontColor(COLOR_DARK)
                .setMarginBottom(2);
        doc.add(t);

        Paragraph d = new Paragraph(desc)
                .setFont(bodyFont).setFontSize(9).setFontColor(COLOR_GRAY)
                .setMarginBottom(4);
        doc.add(d);

        Paragraph genTime = new Paragraph("生成时间: " + LocalDateTime.now().format(DT_FMT))
                .setFont(bodyFont).setFontSize(8).setFontColor(COLOR_GRAY)
                .setMarginBottom(10);
        doc.add(genTime);
    }

    /** 创建基础表格 */
    private Table createTable(float[] colWidths) {
        return new Table(UnitValue.createPercentArray(colWidths))
                .useAllAvailableWidth()
                .setMarginBottom(16);
    }

    /** 表头行 */
    private void addTableHeader(Table table, PdfFont font, String[] headers) {
        for (String h : headers) {
            Cell cell = new Cell()
                    .add(new Paragraph(h).setFont(font).setFontSize(9).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(COLOR_HEADER_BG)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(6)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            table.addHeaderCell(cell);
        }
    }

    /** 数据行（带斑马纹） */
    private void addDataRow(Table table, PdfFont font, String[] cells, int index) {
        addDataRow(table, font, cells, index, null);
    }

    /** 数据行（带斑马纹和可选Aqi颜色标识） */
    private void addDataRow(Table table, PdfFont font, String[] cells, int index, DeviceRgb accentColor) {
        DeviceRgb bg = (index % 2 == 0) ? null : COLOR_STRIPE;
        for (int i = 0; i < cells.length; i++) {
            DeviceRgb textColor = COLOR_TEXT;
            // 如果是AQI等级列（第一列是等级号），用颜色标记
            if (accentColor != null && i == 0) {
                textColor = accentColor;
            }
            Cell cell = new Cell()
                    .add(new Paragraph(cells[i]).setFont(font).setFontSize(8.5f).setFontColor(textColor))
                    .setBorder(Border.NO_BORDER)
                    .setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(
                            new DeviceRgb(230, 230, 230), 0.5f))
                    .setPadding(5)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            if (bg != null) {
                cell.setBackgroundColor(bg);
            }
            table.addCell(cell);
        }
    }

    /** KPI 行 */
    private void addKpiRow(Table table, PdfFont font, String label, String value, String unit) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label).setFont(font).setFontSize(11).setFontColor(COLOR_DARK))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(
                        new DeviceRgb(230, 230, 230), 0.5f))
                .setPadding(8)
                .setTextAlignment(TextAlignment.RIGHT);
        table.addCell(labelCell);

        Cell valueCell = new Cell()
                .add(new Paragraph(value).setFont(font).setFontSize(14).setFontColor(COLOR_PRIMARY))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(
                        new DeviceRgb(230, 230, 230), 0.5f))
                .setPadding(8)
                .setTextAlignment(TextAlignment.CENTER);
        table.addCell(valueCell);

        Cell unitCell = new Cell()
                .add(new Paragraph(unit).setFont(font).setFontSize(10).setFontColor(COLOR_GRAY))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(
                        new DeviceRgb(230, 230, 230), 0.5f))
                .setPadding(8);
        table.addCell(unitCell);
    }

    /** 分隔线组件 */
    private Divider createDivider() {
        return new Divider();
    }

    // ==================== 字体加载 ====================

    private PdfFont loadFont(String name, String encoding) {
        try {
            return PdfFontFactory.createFont(name, encoding, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        } catch (Exception e) {
            // 回退：尝试系统字体
            try {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    return PdfFontFactory.createFont("c:/windows/fonts/simsun.ttc,0",
                            com.itextpdf.io.font.PdfEncodings.IDENTITY_H);
                } else if (os.contains("mac")) {
                    return PdfFontFactory.createFont(
                            "/System/Library/Fonts/STHeiti Light.ttc,0",
                            com.itextpdf.io.font.PdfEncodings.IDENTITY_H);
                }
            } catch (Exception ignored) {}
            // 最终回退：Helvetica（西文only，中文会是方块）
            log.warn("无法加载中文字体，报告中的中文可能无法正常显示");
            try {
                return PdfFontFactory.createFont();
            } catch (java.io.IOException ex) {
                throw new RuntimeException("无法创建任何字体", ex);
            }
        }
    }

    // ==================== 分隔线内部类 ====================

    private static class Divider extends Paragraph {
        Divider() {
            super(new Text("─".repeat(60))
                    .setFontColor(new DeviceRgb(220, 220, 220))
                    .setFontSize(8));
            setMarginBottom(0);
            setMarginTop(0);
        }
    }

    // ==================== 类型转换 ====================

    private String toString(Object obj) { return obj != null ? obj.toString() : ""; }
    private String formatDouble(double v) { return v == (long) v ? String.valueOf((long) v) : String.format("%.1f", v); }
    private double toDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(obj.toString()); } catch (NumberFormatException e) { return 0.0; }
    }
    private int toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number n) return n.intValue();
        try { return Integer.parseInt(obj.toString()); } catch (NumberFormatException e) { return 0; }
    }
    private long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number n) return n.longValue();
        try { return Long.parseLong(obj.toString()); } catch (NumberFormatException e) { return 0L; }
    }
}
