package org.nep.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.nep.common.annotation.RequiresRole;
import org.nep.common.constant.Roles;
import org.nep.common.result.PageResult;
import org.nep.common.result.Result;
import org.nep.common.utils.SecurityUtils;
import org.nep.system.dto.AqiSubmitDTO;
import org.nep.system.entity.AqiDetection;
import org.nep.system.service.AqiDetectionService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AQI 检测管理控制器。
 * <p>/submit、/my、/my/stats 为网格员(NEPG)专属；/page 为管理员(NEPM)确认列表；
 * /{id}、/by-feedback 供登录用户查看检测结果（监督员看自己反馈的检测闭环）。</p>
 *
 * @author NEP Team
 */
@Tag(name = "AQI检测管理")
@RestController
@RequestMapping("/api/aqi")
public class AqiController {

    private final AqiDetectionService aqiService;

    public AqiController(AqiDetectionService s) {
        this.aqiService = s;
    }

    // ==================== 网格员(NEPG)专属 ====================

    @Operation(summary = "提交实测AQI检测（网格员，自动算 AQI=MAX(SO2,CO,PM2.5)）")
    @RequiresRole(Roles.INSPECTOR)
    @PostMapping("/submit")
    public Result<AqiDetection> submit(@Valid @RequestBody AqiSubmitDTO dto) {
        AqiDetection d = new AqiDetection();
        d.setFeedbackId(dto.getFeedbackId());
        d.setSo2Aqi(dto.getSo2Aqi());
        d.setCoAqi(dto.getCoAqi());
        d.setPm25Aqi(dto.getPm25Aqi());
        d.setRemark(dto.getRemark());
        // 检测人以登录态为准，杜绝冒用他人身份提交
        d.setInspectorId(SecurityUtils.getUserId());
        return Result.ok("提交成功", aqiService.submitDetection(d));
    }

    @Operation(summary = "我的检测记录（网格员，来源登录态）")
    @RequiresRole(Roles.INSPECTOR)
    @GetMapping("/my")
    public Result<List<AqiDetection>> myList() {
        LambdaQueryWrapper<AqiDetection> w = new LambdaQueryWrapper<>();
        w.eq(AqiDetection::getInspectorId, SecurityUtils.getUserId())
                .orderByDesc(AqiDetection::getCreateTime);
        return Result.ok(aqiService.list(w));
    }

    @Operation(summary = "我的检测工作量统计（网格员：总数/超标数/达标数）")
    @RequiresRole(Roles.INSPECTOR)
    @GetMapping("/my/stats")
    public Result<Map<String, Object>> myStats() {
        Long uid = SecurityUtils.getUserId();
        List<AqiDetection> mine = aqiService.list(
                new LambdaQueryWrapper<AqiDetection>().eq(AqiDetection::getInspectorId, uid));
        long total = mine.size();
        // AQI > 100 视为超标（对应 AQI 等级 3 级及以上）
        long exceed = mine.stream().filter(d -> d.getFinalAqi() != null && d.getFinalAqi() > 100).count();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("exceed", exceed);
        stats.put("good", total - exceed);
        return Result.ok(stats);
    }

    @Operation(summary = "我的今日检测量（网格员）")
    @RequiresRole(Roles.INSPECTOR)
    @GetMapping("/my/today")
    public Result<Map<String, Object>> myToday() {
        Long uid = SecurityUtils.getUserId();
        String todayStart = java.time.LocalDate.now() + " 00:00:00";
        long todayCount = aqiService.count(new LambdaQueryWrapper<AqiDetection>()
                .eq(AqiDetection::getInspectorId, uid)
                .ge(AqiDetection::getDetectionTime, todayStart));
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("todayCount", todayCount);
        r.put("date", java.time.LocalDate.now().toString());
        return Result.ok(r);
    }

    @Operation(summary = "我的检测记录详情（网格员，仅本人）")
    @RequiresRole(Roles.INSPECTOR)
    @GetMapping("/my/{id}")
    public Result<AqiDetection> myDetail(@PathVariable Long id) {
        AqiDetection d = aqiService.getById(id);
        if (d == null) {
            return Result.notFound("检测记录不存在");
        }
        // 归属校验：仅能查看本人的检测记录
        SecurityUtils.checkOwner(d.getInspectorId());
        return Result.ok(d);
    }

    @Operation(summary = "我的检测记录分页（网格员，可按超标/达标筛选）")
    @RequiresRole(Roles.INSPECTOR)
    @GetMapping("/my/page")
    public PageResult<List<AqiDetection>> myPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String level) {
        LambdaQueryWrapper<AqiDetection> w = new LambdaQueryWrapper<>();
        w.eq(AqiDetection::getInspectorId, SecurityUtils.getUserId());
        // level=exceed 仅看超标(>100)，level=good 仅看达标(<=100)
        if ("exceed".equals(level)) {
            w.gt(AqiDetection::getFinalAqi, 100);
        } else if ("good".equals(level)) {
            w.le(AqiDetection::getFinalAqi, 100);
        }
        w.orderByDesc(AqiDetection::getDetectionTime);
        Page<AqiDetection> r = aqiService.page(new Page<>(page, size), w);
        return PageResult.ok(r.getRecords(), r.getCurrent(), r.getSize(), r.getTotal());
    }

    // ==================== 管理员(NEPM)专属 ====================

    @Operation(summary = "AQI检测分页列表（管理员确认用）")
    @RequiresRole(Roles.ADMIN)
    @GetMapping("/page")
    public PageResult<List<AqiDetection>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        LambdaQueryWrapper<AqiDetection> w = new LambdaQueryWrapper<>();
        w.orderByDesc(AqiDetection::getCreateTime);
        Page<AqiDetection> r = aqiService.page(new Page<>(page, size), w);
        return PageResult.ok(r.getRecords(), r.getCurrent(), r.getSize(), r.getTotal());
    }

    @Operation(summary = "AQI检测全局统计（管理员：总数/超标数/超标率）")
    @RequiresRole(Roles.ADMIN)
    @GetMapping("/stats/overview")
    public Result<Map<String, Object>> overviewStats() {
        List<AqiDetection> all = aqiService.list();
        long total = all.size();
        // AQI > 100 视为超标（轻度污染及以上）
        long exceed = all.stream().filter(d -> d.getFinalAqi() != null && d.getFinalAqi() > 100).count();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("exceed", exceed);
        stats.put("exceedRate", total > 0 ? String.format("%.1f%%", exceed * 100.0 / total) : "0.0%");
        return Result.ok(stats);
    }

    // ==================== 共享：详情查询 ====================

    @Operation(summary = "AQI检测详情")
    @GetMapping("/{id}")
    public Result<AqiDetection> getById(@PathVariable Long id) {
        return Result.ok(aqiService.getById(id));
    }

    @Operation(summary = "根据反馈ID获取AQI检测数据（供监督员/管理员查看闭环结果）")
    @GetMapping("/by-feedback/{feedbackId}")
    public Result<AqiDetection> getByFeedbackId(@PathVariable Long feedbackId) {
        LambdaQueryWrapper<AqiDetection> w = new LambdaQueryWrapper<>();
        w.eq(AqiDetection::getFeedbackId, feedbackId);
        return Result.ok(aqiService.getOne(w));
    }
}
