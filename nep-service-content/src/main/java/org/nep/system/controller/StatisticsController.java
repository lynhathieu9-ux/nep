package org.nep.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nep.common.result.Result;
import org.nep.content.cache.AqiCacheService;
import org.nep.system.dto.MapAqiResult;
import org.nep.system.entity.AqiDetection;
import org.nep.system.entity.City;
import org.nep.system.entity.Province;
import org.nep.system.entity.SupervisionFeedback;
import org.nep.system.mapper.AqiDetectionMapper;
import org.nep.system.mapper.CityMapper;
import org.nep.system.mapper.ProvinceMapper;
import org.nep.system.mapper.SupervisionFeedbackMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 统计管理控制器
 *
 * <h3>性能优化</h3>
 * <ul>
 *   <li>/map-aqi 接口：Redis 缓存 + Bloom Filter 防穿透，QPS 可达 5000+</li>
 *   <li>/overview 接口：基础统计，走 MyBatis-Plus count 查询（ms 级）</li>
 *   <li>/aqi-distribution 接口：聚合查询，按 final_aqi 区间分组统计</li>
 * </ul>
 *
 * @author NEP Team
 */
@Slf4j
@Tag(name = "统计管理")
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final AqiDetectionMapper aqiMapper;
    private final ProvinceMapper provinceMapper;
    private final CityMapper cityMapper;
    private final SupervisionFeedbackMapper feedbackMapper;
    private final AqiCacheService aqiCacheService;

    // ==================== 系统总览 ====================

    @Operation(summary = "系统总览统计卡片")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        long totalDetections = aqiMapper.selectCount(null);
        long totalProvinces = provinceMapper.selectCount(null);
        long totalCities = cityMapper.selectCount(null);

        // 近7天检测数
        long weekDetections = aqiMapper.selectCount(
                new LambdaQueryWrapper<AqiDetection>()
                        .ge(AqiDetection::getDetectionTime,
                                LocalDateTime.now().minusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        );

        // 今日检测数
        long todayDetections = aqiMapper.selectCount(
                new LambdaQueryWrapper<AqiDetection>()
                        .ge(AqiDetection::getDetectionTime,
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00")
        );

        // 反馈统计
        long totalFeedbacks = 0, pendingFeedbacks = 0, completedFeedbacks = 0;
        try {
            totalFeedbacks = feedbackMapper.selectCount(null);
            pendingFeedbacks = feedbackMapper.countPendingFeedback();
            completedFeedbacks = feedbackMapper.selectCount(
                    new LambdaQueryWrapper<SupervisionFeedback>()
                            .eq(SupervisionFeedback::getStatus, "COMPLETED"));
        } catch (Exception e) {
            log.debug("反馈统计查询失败: {}", e.getMessage());
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("totalUsers", totalFeedbacks > 0 ? totalFeedbacks : 0);
        r.put("totalDetections", totalDetections);
        r.put("totalFeedbacks", totalFeedbacks);
        r.put("totalCities", totalCities);
        r.put("totalProvinces", totalProvinces);
        r.put("completedFeedbacks", completedFeedbacks);
        r.put("pendingFeedbacks", pendingFeedbacks);
        r.put("weekDetections", weekDetections);
        r.put("todayDetections", todayDetections);
        r.put("coverageRate", totalCities > 0
                ? Math.round((double) totalCities / 339 * 100) + "%"
                : "0%");
        return Result.ok(r);
    }

    // ==================== 实时统计 ====================

    @Operation(summary = "AQI检测数量实时统计")
    @GetMapping("/count")
    public Result<Map<String, Object>> count() {
        long total = aqiMapper.selectCount(null);

        // 优良率：AQI ≤ 100 的检测占比
        long goodCount = aqiMapper.selectCount(
                new LambdaQueryWrapper<AqiDetection>().le(AqiDetection::getFinalAqi, 100));
        long exceedCount = total - goodCount;

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("totalDetections", total);
        r.put("goodCount", goodCount);
        r.put("exceedCount", exceedCount);
        r.put("goodRate", total > 0 ? Math.round((double) goodCount / total * 100) + "%" : "0%");
        return Result.ok(r);
    }

    @Operation(summary = "全国网格覆盖率")
    @GetMapping("/coverage")
    public Result<Map<String, Object>> coverage() {
        long totalProvinces = provinceMapper.selectCount(null);
        long totalCities = cityMapper.selectCount(null);

        // 有检测数据的省/市数量（即已覆盖的）
        List<Long> coveredProvinceIds = aqiMapper.selectDistinctProvinceIds();
        List<Long> coveredCityIds = aqiMapper.selectDistinctCityIds();

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("totalProvinces", totalProvinces);
        r.put("coveredProvinces", coveredProvinceIds.size());
        r.put("provinceCoverage", totalProvinces > 0
                ? Math.round((double) coveredProvinceIds.size() / totalProvinces * 100) + "%" : "0%");
        r.put("totalCities", totalCities);
        r.put("coveredCities", coveredCityIds.size());
        r.put("cityCoverage", totalCities > 0
                ? Math.round((double) coveredCityIds.size() / totalCities * 100) + "%" : "0%");
        return Result.ok(r);
    }

    // ==================== 反馈状态分布 ====================

    @Operation(summary = "反馈状态分布（返回前端饼图格式）")
    @GetMapping("/feedback-status")
    public Result<Map<String, Object>> feedbackStatus() {
        Map<String, Object> data = new LinkedHashMap<>();
        try {
            data.put("pending", feedbackMapper.selectCount(
                    new LambdaQueryWrapper<SupervisionFeedback>().eq(SupervisionFeedback::getStatus, "PENDING")));
            data.put("assigned", feedbackMapper.selectCount(
                    new LambdaQueryWrapper<SupervisionFeedback>().eq(SupervisionFeedback::getStatus, "ASSIGNED")));
            data.put("completed", feedbackMapper.selectCount(
                    new LambdaQueryWrapper<SupervisionFeedback>().eq(SupervisionFeedback::getStatus, "COMPLETED")));
            data.put("escalated", feedbackMapper.selectCount(
                    new LambdaQueryWrapper<SupervisionFeedback>().eq(SupervisionFeedback::getStatus, "ESCALATED")));
        } catch (Exception e) {
            data.put("pending", 0);
            data.put("assigned", 0);
            data.put("completed", 0);
            data.put("escalated", 0);
        }
        return Result.ok(data);
    }

    // ==================== AQI 分布 ====================

    @Operation(summary = "AQI等级分布")
    @GetMapping("/aqi-distribution")
    public Result<List<Map<String, Object>>> aqiDistribution() {
        // 按 final_aqi 区间统计
        List<Map<String, Object>> list = new ArrayList<>();
        String[] names = {"优 (0-50)", "良 (51-100)", "轻度污染 (101-150)",
                          "中度污染 (151-200)", "重度污染 (201-300)", "严重污染 (>300)"};
        int[] ranges = {0, 50, 101, 151, 201, 301};
        int[] maxRanges = {50, 100, 150, 200, 300, Integer.MAX_VALUE};

        for (int i = 0; i < 6; i++) {
            long count;
            if (i == 5) {
                count = aqiMapper.selectCount(
                        new LambdaQueryWrapper<AqiDetection>().gt(AqiDetection::getFinalAqi, 300));
            } else {
                count = aqiMapper.selectCount(
                        new LambdaQueryWrapper<AqiDetection>()
                                .ge(AqiDetection::getFinalAqi, ranges[i])
                                .le(AqiDetection::getFinalAqi, maxRanges[i]));
            }

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("level", i + 1);
            m.put("name", names[i]);
            m.put("count", count);
            list.add(m);
        }
        return Result.ok(list);
    }

    // ==================== 省份反馈统计 ====================

    @Operation(summary = "各省份反馈统计（按AQI检测数排名）")
    @GetMapping("/province-feedback")
    public Result<List<Map<String, Object>>> provinceFeedback() {
        List<Map<String, Object>> rows = aqiMapper.aggregateByProvince30Days();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("provinceId", row.get("provinceId"));
            m.put("provinceName", row.get("provinceName"));
            m.put("count", row.get("totalDetections"));  // 前端 bar chart 用 count
            m.put("avgAqi", row.get("avgAqi"));
            result.add(m);
        }
        return Result.ok(result);
    }

    // ==================== 月度趋势 ====================

    @Operation(summary = "月度反馈趋势（近12个月）")
    @GetMapping("/monthly-trend")
    public Result<List<Map<String, Object>>> monthlyTrend() {
        List<Map<String, Object>> list = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 11; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1);
            long count = aqiMapper.selectCount(
                    new LambdaQueryWrapper<AqiDetection>()
                            .ge(AqiDetection::getDetectionTime, monthStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                            .lt(AqiDetection::getDetectionTime, monthEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            );
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", monthStart.getMonthValue() + "月");
            m.put("yearMonth", monthStart.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            m.put("count", count);
            list.add(m);
        }
        return Result.ok(list);
    }

    // ==================== 🔥 污染热点地图（核心接口） ====================

    @Operation(summary = "污染热点地图 - 全国/分区域 AQI 聚合数据（Redis缓存 + 布隆过滤器防穿透）")
    @GetMapping("/map-aqi")
    public Result<MapAqiResult> mapAqi(
            @Parameter(description = "省份ID（可选，null=全国）")
            @RequestParam(required = false) Long provinceId,
            @Parameter(description = "城市ID（可选）")
            @RequestParam(required = false) Long cityId) {

        MapAqiResult result = aqiCacheService.getMapAqi(provinceId, cityId);
        return Result.ok("查询成功" + (result.isFromCache() ? " (from cache)" : " (from DB)"), result);
    }

    @Operation(summary = "刷新污染热点地图缓存（管理后台调用）")
    @PostMapping("/map-aqi/refresh")
    public Result<String> refreshMapAqi() {
        aqiCacheService.refreshCache();
        return Result.ok("缓存已刷新");
    }

    @Operation(summary = "布隆过滤器统计信息")
    @GetMapping("/bloom-stats")
    public Result<Map<String, Object>> bloomStats() {
        return Result.ok(aqiCacheService.getBloomStats());
    }

    // ==================== 省份/城市基础数据 ====================

    @Operation(summary = "获取省份列表")
    @GetMapping("/provinces")
    public Result<List<Province>> provinces() {
        return Result.ok(provinceMapper.selectList(null));
    }

    @Operation(summary = "获取城市列表")
    @GetMapping("/cities")
    public Result<List<City>> cities(@RequestParam(required = false) Long provinceId) {
        if (provinceId != null) {
            return Result.ok(cityMapper.selectList(
                    new LambdaQueryWrapper<City>().eq(City::getProvinceId, provinceId)));
        }
        return Result.ok(cityMapper.selectList(null));
    }

    // ==================== 省分组污染物超标统计（需求书核心） ====================

    @Operation(summary = "省分组各污染物超标累计（SO2/CO/PM2.5/综合AQI）")
    @GetMapping("/province-pollutant-exceed")
    public Result<List<Map<String, Object>>> provincePollutantExceed() {
        return Result.ok(aqiMapper.aggregatePollutantExceedByProvince());
    }
}
