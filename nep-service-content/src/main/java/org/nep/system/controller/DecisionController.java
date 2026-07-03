package org.nep.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nep.common.annotation.RequiresRole;
import org.nep.common.constant.Roles;
import org.nep.common.result.Result;
import org.nep.system.entity.AqiDetection;
import org.nep.system.entity.SupervisionFeedback;
import org.nep.system.mapper.AqiDetectionMapper;
import org.nep.system.mapper.CityMapper;
import org.nep.system.mapper.ProvinceMapper;
import org.nep.system.mapper.SupervisionFeedbackMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 决策者(NEPV)专属统计与决策支持控制器。
 * <p>面向环保决策者提供全局视角的数据服务：省分组污染物超标、AQI 分布/趋势、
 * 网格覆盖率、实时检测量，以及决策专属的综合决策报告与重点关注省份预警。
 * 全部接口限定决策者与管理员访问（{@code @RequiresRole({NEPV, NEPM})}），
 * 数据均来自真实业务表 nep_aqi_detection / nep_supervision_feedback，保证决策依据真实可溯。</p>
 *
 * @author NEP Team
 */
@Tag(name = "决策者统计支持")
@RestController
@RequestMapping("/api/decision")
@RequiredArgsConstructor
public class DecisionController {

    private final AqiDetectionMapper aqiMapper;
    private final SupervisionFeedbackMapper feedbackMapper;
    private final ProvinceMapper provinceMapper;
    private final CityMapper cityMapper;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 需求书 8 类统计（决策者视角） ====================

    @Operation(summary = "决策总览（关键指标汇总）")
    @RequiresRole({Roles.DECISION, Roles.ADMIN})
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> r = new LinkedHashMap<>();
        long totalDetections = aqiMapper.selectCount(null);
        r.put("totalDetections", totalDetections);
        r.put("totalFeedbacks", feedbackMapper.selectCount(null));
        r.put("totalProvinces", provinceMapper.selectCount(null));
        r.put("totalCities", cityMapper.selectCount(null));
        long exceed = aqiMapper.selectCount(new LambdaQueryWrapper<AqiDetection>().gt(AqiDetection::getFinalAqi, 100));
        r.put("exceedDetections", exceed);
        r.put("exceedRate", totalDetections > 0 ? Math.round(exceed * 100.0 / totalDetections) + "%" : "0%");
        return Result.ok(r);
    }

    @Operation(summary = "省分组各污染物超标累计（SO2/CO/PM2.5/综合AQI）")
    @RequiresRole({Roles.DECISION, Roles.ADMIN})
    @GetMapping("/province-pollutant-exceed")
    public Result<List<Map<String, Object>>> provincePollutantExceed() {
        return Result.ok(aqiMapper.aggregatePollutantExceedByProvince());
    }

    @Operation(summary = "AQI 等级分布统计")
    @RequiresRole({Roles.DECISION, Roles.ADMIN})
    @GetMapping("/aqi-distribution")
    public Result<List<Map<String, Object>>> aqiDistribution() {
        List<Map<String, Object>> list = new ArrayList<>();
        String[] names = {"优", "良", "轻度污染", "中度污染", "重度污染", "严重污染"};
        int[] lo = {0, 51, 101, 151, 201, 301};
        int[] hi = {50, 100, 150, 200, 300, Integer.MAX_VALUE};
        for (int i = 0; i < 6; i++) {
            long count = (i == 5)
                    ? aqiMapper.selectCount(new LambdaQueryWrapper<AqiDetection>().gt(AqiDetection::getFinalAqi, 300))
                    : aqiMapper.selectCount(new LambdaQueryWrapper<AqiDetection>()
                            .ge(AqiDetection::getFinalAqi, lo[i]).le(AqiDetection::getFinalAqi, hi[i]));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("level", i + 1);
            m.put("name", names[i]);
            m.put("count", count);
            list.add(m);
        }
        return Result.ok(list);
    }

    @Operation(summary = "AQI 趋势统计（近12个月检测量）")
    @RequiresRole({Roles.DECISION, Roles.ADMIN})
    @GetMapping("/monthly-trend")
    public Result<List<Map<String, Object>>> monthlyTrend() {
        List<Map<String, Object>> list = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 11; i >= 0; i--) {
            LocalDateTime start = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime end = start.plusMonths(1);
            long count = aqiMapper.selectCount(new LambdaQueryWrapper<AqiDetection>()
                    .ge(AqiDetection::getDetectionTime, start.format(DT))
                    .lt(AqiDetection::getDetectionTime, end.format(DT)));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", start.getMonthValue() + "月");
            m.put("count", count);
            list.add(m);
        }
        return Result.ok(list);
    }

    @Operation(summary = "全国网格覆盖率（省/市覆盖）")
    @RequiresRole({Roles.DECISION, Roles.ADMIN})
    @GetMapping("/coverage")
    public Result<Map<String, Object>> coverage() {
        long totalProvinces = provinceMapper.selectCount(null);
        long totalCities = cityMapper.selectCount(null);
        int coveredProvinces = aqiMapper.selectDistinctProvinceIds().size();
        int coveredCities = aqiMapper.selectDistinctCityIds().size();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("totalProvinces", totalProvinces);
        r.put("coveredProvinces", coveredProvinces);
        r.put("provinceCoverage", totalProvinces > 0 ? Math.round(coveredProvinces * 100.0 / totalProvinces) + "%" : "0%");
        r.put("totalCities", totalCities);
        r.put("coveredCities", coveredCities);
        r.put("cityCoverage", totalCities > 0 ? Math.round(coveredCities * 100.0 / totalCities) + "%" : "0%");
        return Result.ok(r);
    }

    @Operation(summary = "空气质量检测数量实时统计")
    @RequiresRole({Roles.DECISION, Roles.ADMIN})
    @GetMapping("/detection-count")
    public Result<Map<String, Object>> detectionCount() {
        long total = aqiMapper.selectCount(null);
        long good = aqiMapper.selectCount(new LambdaQueryWrapper<AqiDetection>().le(AqiDetection::getFinalAqi, 100));
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", total);
        r.put("good", good);
        r.put("exceed", total - good);
        r.put("goodRate", total > 0 ? Math.round(good * 100.0 / total) + "%" : "0%");
        return Result.ok(r);
    }

    @Operation(summary = "各省份检测量排名（前10）")
    @RequiresRole({Roles.DECISION, Roles.ADMIN})
    @GetMapping("/province-ranking")
    public Result<List<Map<String, Object>>> provinceRanking() {
        List<Map<String, Object>> all = aqiMapper.aggregateByProvince30Days();
        return Result.ok(all.size() > 10 ? all.subList(0, 10) : all);
    }

    @Operation(summary = "反馈处理状态分布")
    @RequiresRole({Roles.DECISION, Roles.ADMIN})
    @GetMapping("/feedback-status")
    public Result<Map<String, Object>> feedbackStatus() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("pending", countStatus("PENDING"));
        r.put("assigned", countStatus("ASSIGNED"));
        r.put("completed", countStatus("COMPLETED"));
        r.put("escalated", countStatus("ESCALATED"));
        return Result.ok(r);
    }

    // ==================== 决策者专属高阶功能 ====================

    @Operation(summary = "重点关注省份预警（超标最严重前5省，供决策优先干预）")
    @RequiresRole({Roles.DECISION, Roles.ADMIN})
    @GetMapping("/focus-provinces")
    public Result<List<Map<String, Object>>> focusProvinces() {
        List<Map<String, Object>> all = aqiMapper.aggregatePollutantExceedByProvince();
        List<Map<String, Object>> top = new ArrayList<>();
        for (Map<String, Object> row : all) {
            long aqiExceed = row.get("aqiExceed") == null ? 0 : ((Number) row.get("aqiExceed")).longValue();
            if (aqiExceed > 0) {
                Map<String, Object> m = new LinkedHashMap<>(row);
                // 预警级别：超标数越多级别越高
                m.put("warnLevel", aqiExceed >= 10 ? "严重" : (aqiExceed >= 5 ? "较重" : "一般"));
                top.add(m);
            }
            if (top.size() >= 5) break;
        }
        return Result.ok(top);
    }

    @Operation(summary = "综合决策报告（一次性汇总核心决策指标）")
    @RequiresRole({Roles.DECISION, Roles.ADMIN})
    @GetMapping("/report")
    public Result<Map<String, Object>> report() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("overview", overview().getData());
        report.put("aqiDistribution", aqiDistribution().getData());
        report.put("coverage", coverage().getData());
        report.put("focusProvinces", focusProvinces().getData());
        report.put("generatedAt", LocalDateTime.now().format(DT));
        return Result.ok(report);
    }

    private long countStatus(String status) {
        return feedbackMapper.selectCount(
                new LambdaQueryWrapper<SupervisionFeedback>().eq(SupervisionFeedback::getStatus, status));
    }
}
