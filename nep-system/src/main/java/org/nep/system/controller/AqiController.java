package org.nep.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nep.common.result.PageResult;
import org.nep.common.result.Result;
import org.nep.system.entity.AqiDetection;
import org.nep.system.service.AqiDetectionService;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AQI检测管理")
@RestController
@RequestMapping("/api/aqi")
public class AqiController {

    private final AqiDetectionService aqiService;
    public AqiController(AqiDetectionService s) { this.aqiService = s; }

    @Operation(summary = "提交AQI检测")
    @PostMapping("/submit")
    public Result<AqiDetection> submit(@RequestBody AqiDetection d) {
        return Result.ok("提交成功", aqiService.submitDetection(d));
    }

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public PageResult<java.util.List<AqiDetection>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        LambdaQueryWrapper<AqiDetection> w = new LambdaQueryWrapper<>();
        w.orderByDesc(AqiDetection::getCreateTime);
        Page<AqiDetection> r = aqiService.page(new Page<>(page, size), w);
        return PageResult.ok(r.getRecords(), r.getCurrent(), r.getSize(), r.getTotal());
    }

    @Operation(summary = "详情")
    @GetMapping("/{id}")
    public Result<AqiDetection> getById(@PathVariable Long id) { return Result.ok(aqiService.getById(id)); }

    @Operation(summary = "我的检测记录")
    @GetMapping("/my/{inspectorId}")
    public Result<java.util.List<AqiDetection>> my(@PathVariable Long inspectorId) {
        LambdaQueryWrapper<AqiDetection> w = new LambdaQueryWrapper<>();
        w.eq(AqiDetection::getInspectorId, inspectorId).orderByDesc(AqiDetection::getCreateTime);
        return Result.ok(aqiService.list(w));
    }
}
