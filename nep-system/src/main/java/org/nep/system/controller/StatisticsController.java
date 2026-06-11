package org.nep.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nep.common.result.Result;
import org.nep.system.mapper.AqiDetectionMapper;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Tag(name = "统计管理")
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final AqiDetectionMapper aqiMapper;
    public StatisticsController(AqiDetectionMapper m) { this.aqiMapper = m; }

    @Operation(summary = "AQI检测数量实时统计")
    @GetMapping("/count")
    public Result<Map<String, Object>> count() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("totalDetections", aqiMapper.selectCount(null));
        r.put("goodCount", 0);
        r.put("exceedCount", 0);
        return Result.ok(r);
    }

    @Operation(summary = "全国网格覆盖率")
    @GetMapping("/coverage")
    public Result<Map<String, Object>> coverage() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("provinceCoverage", "0%");
        r.put("cityCoverage", "0%");
        return Result.ok(r);
    }
}
