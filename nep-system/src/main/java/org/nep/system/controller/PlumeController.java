package org.nep.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nep.common.result.Result;
import org.nep.system.entity.SupervisionFeedback;
import org.nep.system.mapper.SupervisionFeedbackMapper;
import org.nep.system.service.PlumeModelService;
import org.nep.system.service.PlumeModelService.StabilityClass;
import org.nep.system.util.GeoHashUtil;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "污染扩散模拟与溯源分析")
@RestController
@RequestMapping("/api/plume")
public class PlumeController {

    private final PlumeModelService plumeService;
    private final SupervisionFeedbackMapper feedbackMapper;

    public PlumeController(PlumeModelService plumeService, SupervisionFeedbackMapper feedbackMapper) {
        this.plumeService = plumeService;
        this.feedbackMapper = feedbackMapper;
    }

    @Operation(summary = "高斯扩散模拟 — 计算污染扩散网格")
    @PostMapping("/simulate")
    public Result<Map<String, Object>> simulate(@RequestBody Map<String, Object> params) {
        double sourceLng = ((Number) params.getOrDefault("sourceLng", 116.4)).doubleValue();
        double sourceLat = ((Number) params.getOrDefault("sourceLat", 39.9)).doubleValue();
        double sourceStrength = ((Number) params.getOrDefault("sourceStrength", 100)).doubleValue();
        double windSpeed = ((Number) params.getOrDefault("windSpeed", 3.0)).doubleValue();
        double windDirection = ((Number) params.getOrDefault("windDirection", 180)).doubleValue();
        double effectiveHeight = ((Number) params.getOrDefault("effectiveHeight", 10)).doubleValue();
        String stabilityStr = (String) params.getOrDefault("stabilityClass", "D");
        int gridSize = ((Number) params.getOrDefault("gridSize", 40)).intValue();
        double stepKm = ((Number) params.getOrDefault("stepKm", 0.5)).doubleValue();

        StabilityClass stability;
        try {
            stability = StabilityClass.valueOf(stabilityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            stability = StabilityClass.D;
        }

        List<Map<String, Object>> grid = plumeService.computePlumeGrid(
                sourceLng, sourceLat, sourceStrength, windSpeed,
                windDirection, effectiveHeight, stability, gridSize, stepKm);

        // 计算羽流中心线 (下风向轴线)
        List<double[]> centerline = new ArrayList<>();
        for (int i = 0; i <= 20; i++) {
            double distKm = i * stepKm * 2;
            double angleRad = Math.toRadians(90 - windDirection);
            double lng = sourceLng + (distKm / 111.0) * Math.cos(angleRad);
            double lat = sourceLat + (distKm / 111.0) * Math.sin(angleRad);
            double conc = plumeService.concentration(sourceLng, sourceLat, lng, lat,
                    sourceStrength, windSpeed, windDirection, effectiveHeight, stability);
            if (conc > 0.0001) {
                centerline.add(new double[]{lng, lat, conc});
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", Map.of("lng", sourceLng, "lat", sourceLat));
        result.put("grid", grid);
        result.put("centerline", centerline);
        result.put("params", Map.of(
                "windSpeed", windSpeed, "windDirection", windDirection,
                "stabilityClass", stabilityStr, "effectiveHeight", effectiveHeight));

        return Result.ok(result);
    }

    @Operation(summary = "反向溯源 — 基于多点观测数据估算污染源位置")
    @PostMapping("/trace")
    public Result<Map<String, Object>> traceSource(@RequestBody List<Map<String, Object>> observations) {
        Map<String, Object> source = plumeService.sourceTracing(observations);
        return Result.ok(source);
    }

    @Operation(summary = "附近反馈查询 — 根据经纬度查找周边反馈")
    @GetMapping("/nearby-feedbacks")
    public Result<List<Map<String, Object>>> nearbyFeedbacks(
            @RequestParam Double lng,
            @RequestParam Double lat,
            @RequestParam(defaultValue = "5000") Integer radius) {

        List<SupervisionFeedback> all = feedbackMapper.selectList(null);
        List<Map<String, Object>> nearby = new ArrayList<>();

        for (SupervisionFeedback f : all) {
            if (f.getLongitude() == null || f.getLatitude() == null) continue;
            double dist = GeoHashUtil.distance(lat, lng, f.getLatitude(), f.getLongitude());
            if (dist <= radius) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", f.getId());
                item.put("lng", f.getLongitude());
                item.put("lat", f.getLatitude());
                item.put("address", f.getSpecificAddress());
                item.put("aqiLevel", f.getEstimatedAqiLevel());
                item.put("status", f.getStatus());
                item.put("description", f.getDescription());
                item.put("distance", Math.round(dist));
                item.put("createTime", f.getCreateTime());
                nearby.add(item);
            }
        }

        nearby.sort(Comparator.comparingDouble(m ->
                ((Number) m.get("distance")).doubleValue()));

        return Result.ok(nearby);
    }

    @Operation(summary = "反馈经纬度统计 — 所有带坐标的反馈点(用于地图标记)")
    @GetMapping("/geo-feedbacks")
    public Result<List<Map<String, Object>>> geoFeedbacks() {
        List<SupervisionFeedback> all = feedbackMapper.selectList(null);
        List<Map<String, Object>> points = new ArrayList<>();

        for (SupervisionFeedback f : all) {
            if (f.getLongitude() == null || f.getLatitude() == null) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", f.getId());
            item.put("lng", f.getLongitude());
            item.put("lat", f.getLatitude());
            item.put("aqiLevel", f.getEstimatedAqiLevel());
            item.put("status", f.getStatus());
            points.add(item);
        }

        return Result.ok(points);
    }
}
