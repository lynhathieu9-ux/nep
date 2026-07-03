package org.nep.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nep.common.annotation.RequiresRole;
import org.nep.common.constant.Roles;
import org.nep.common.exception.BusinessException;
import org.nep.common.result.PageResult;
import org.nep.common.result.Result;
import org.nep.common.utils.SecurityUtils;
import org.nep.feedback.client.UserClient;
import org.nep.system.dto.FeedbackRateDTO;
import org.nep.system.dto.FeedbackSubmitDTO;
import org.nep.system.entity.SupervisionFeedback;
import org.nep.system.service.SupervisionFeedbackService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 监督反馈管理控制器。
 * <p>按角色划分：/submit、/my*、/cancel、/{id}(修改) 为监督员(NEPS)专属；
 * /page、/assign、/escalate 等调度类接口为管理员(NEPM)专属。
 * 监督员相关接口一律以登录态用户ID为准，并做数据归属校验，杜绝越权与冒名。</p>
 *
 * @author NEP Team
 */
@Tag(name = "监督反馈管理")
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final SupervisionFeedbackService feedbackService;
    /** 用户微服务 Feign 客户端（问题①：按姓名派送时做姓名→ID映射） */
    private final UserClient userClient;

    // ==================== 监督员(NEPS)专属 ====================

    @Operation(summary = "提交监督反馈（监督员）")
    @RequiresRole(Roles.SUPERVISOR)
    @PostMapping("/submit")
    public Result<SupervisionFeedback> submit(@Valid @RequestBody FeedbackSubmitDTO dto) {
        SupervisionFeedback f = new SupervisionFeedback();
        f.setProvinceId(dto.getProvinceId());
        f.setCityId(dto.getCityId());
        f.setSpecificAddress(dto.getSpecificAddress());
        f.setEstimatedAqiLevel(dto.getEstimatedAqiLevel());
        f.setDescription(dto.getDescription());
        // 提交者以登录态为准，前端无法伪造他人身份
        f.setSupervisorId(SecurityUtils.getUserId());
        return Result.ok("反馈提交成功", feedbackService.submit(f));
    }

    @Operation(summary = "我的反馈列表（监督员，来源登录态）")
    @RequiresRole(Roles.SUPERVISOR)
    @GetMapping("/my")
    public Result<List<SupervisionFeedback>> myList() {
        LambdaQueryWrapper<SupervisionFeedback> w = new LambdaQueryWrapper<>();
        w.eq(SupervisionFeedback::getSupervisorId, SecurityUtils.getUserId())
                .orderByDesc(SupervisionFeedback::getCreateTime);
        return Result.ok(feedbackService.list(w));
    }

    @Operation(summary = "我的反馈分页（监督员，支持状态/关键词/日期筛选）")
    @RequiresRole(Roles.SUPERVISOR)
    @GetMapping("/my/page")
    public PageResult<List<SupervisionFeedback>> myPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LambdaQueryWrapper<SupervisionFeedback> w = new LambdaQueryWrapper<>();
        // 作用域强制限定为当前登录监督员本人
        w.eq(SupervisionFeedback::getSupervisorId, SecurityUtils.getUserId());
        if (status != null && !status.isEmpty()) {
            w.eq(SupervisionFeedback::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            w.and(qw -> qw.like(SupervisionFeedback::getDescription, keyword)
                    .or().like(SupervisionFeedback::getSpecificAddress, keyword));
        }
        if (startDate != null && !startDate.isEmpty()) {
            w.ge(SupervisionFeedback::getCreateTime, startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            w.le(SupervisionFeedback::getCreateTime, endDate + " 23:59:59");
        }
        w.orderByDesc(SupervisionFeedback::getCreateTime);
        Page<SupervisionFeedback> r = feedbackService.page(new Page<>(page, size), w);
        return PageResult.ok(r.getRecords(), r.getCurrent(), r.getSize(), r.getTotal());
    }

    @Operation(summary = "我的反馈统计（监督员：总数/各状态数量）")
    @RequiresRole(Roles.SUPERVISOR)
    @GetMapping("/my/stats")
    public Result<Map<String, Object>> myStats() {
        Long uid = SecurityUtils.getUserId();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", countMine(uid, null));
        stats.put("pending", countMine(uid, SupervisionFeedback.STATUS_PENDING));
        stats.put("assigned", countMine(uid, SupervisionFeedback.STATUS_ASSIGNED));
        stats.put("completed", countMine(uid, SupervisionFeedback.STATUS_COMPLETED));
        stats.put("escalated", countMine(uid, SupervisionFeedback.STATUS_ESCALATED));
        return Result.ok(stats);
    }

    /** 统计当前监督员在某状态下的反馈数量（status 为 null 表示全部） */
    private long countMine(Long supervisorId, String status) {
        LambdaQueryWrapper<SupervisionFeedback> w = new LambdaQueryWrapper<>();
        w.eq(SupervisionFeedback::getSupervisorId, supervisorId);
        if (status != null) {
            w.eq(SupervisionFeedback::getStatus, status);
        }
        return feedbackService.count(w);
    }

    @Operation(summary = "我的反馈详情（监督员，含归属校验）")
    @RequiresRole(Roles.SUPERVISOR)
    @GetMapping("/my/detail/{id}")
    public Result<SupervisionFeedback> myDetail(@PathVariable Long id) {
        SupervisionFeedback f = feedbackService.getById(id);
        if (f == null) {
            throw new BusinessException("反馈记录不存在");
        }
        // 仅能查看本人反馈
        SecurityUtils.checkOwner(f.getSupervisorId());
        return Result.ok(f);
    }

    @Operation(summary = "我的月度提交趋势（监督员，近6个月）")
    @RequiresRole(Roles.SUPERVISOR)
    @GetMapping("/my/trend")
    public Result<List<Map<String, Object>>> myTrend() {
        Long uid = SecurityUtils.getUserId();
        List<SupervisionFeedback> all = feedbackService.list(
                new LambdaQueryWrapper<SupervisionFeedback>().eq(SupervisionFeedback::getSupervisorId, uid));
        List<Map<String, Object>> trend = new ArrayList<>();
        java.time.LocalDate now = java.time.LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            java.time.YearMonth ym = java.time.YearMonth.from(now.minusMonths(i));
            long count = all.stream().filter(f -> f.getCreateTime() != null
                    && java.time.YearMonth.from(f.getCreateTime()).equals(ym)).count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", ym.getMonthValue() + "月");
            m.put("count", count);
            trend.add(m);
        }
        return Result.ok(trend);
    }

    @Operation(summary = "我的关注网格区域分布（监督员，按城市分组我的反馈数）")
    @RequiresRole(Roles.SUPERVISOR)
    @GetMapping("/my/regions")
    public Result<List<Map<String, Object>>> myRegions() {
        Long uid = SecurityUtils.getUserId();
        List<SupervisionFeedback> all = feedbackService.list(
                new LambdaQueryWrapper<SupervisionFeedback>().eq(SupervisionFeedback::getSupervisorId, uid));
        Map<Long, Long> byCity = new LinkedHashMap<>();
        for (SupervisionFeedback f : all) {
            if (f.getCityId() != null) {
                byCity.merge(f.getCityId(), 1L, Long::sum);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        byCity.forEach((cityId, cnt) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("cityId", cityId);
            m.put("count", cnt);
            result.add(m);
        });
        return Result.ok(result);
    }

    @Operation(summary = "修改待指派反馈（监督员，仅PENDING可改）")
    @RequiresRole(Roles.SUPERVISOR)
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody FeedbackSubmitDTO dto) {
        SupervisionFeedback f = feedbackService.getById(id);
        if (f == null) {
            throw new BusinessException("反馈记录不存在");
        }
        SecurityUtils.checkOwner(f.getSupervisorId());
        if (!SupervisionFeedback.STATUS_PENDING.equals(f.getStatus())) {
            throw new BusinessException("仅待指派状态的反馈可修改");
        }
        f.setProvinceId(dto.getProvinceId());
        f.setCityId(dto.getCityId());
        f.setSpecificAddress(dto.getSpecificAddress());
        f.setEstimatedAqiLevel(dto.getEstimatedAqiLevel());
        f.setDescription(dto.getDescription());
        feedbackService.updateById(f);
        return Result.success("修改成功");
    }

    @Operation(summary = "撤回反馈（监督员，仅PENDING状态）")
    @RequiresRole(Roles.SUPERVISOR)
    @PutMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        // 归属以登录态为准，仅能撤回本人反馈
        feedbackService.cancel(id, SecurityUtils.getUserId());
        return Result.success("反馈已撤回");
    }

    @Operation(summary = "评价已完成反馈（监督员，仅COMPLETED且本人）")
    @RequiresRole(Roles.SUPERVISOR)
    @PutMapping("/rate/{id}")
    public Result<Void> rate(@PathVariable Long id, @Valid @RequestBody FeedbackRateDTO dto) {
        SupervisionFeedback f = feedbackService.getById(id);
        if (f == null) {
            throw new BusinessException("反馈记录不存在");
        }
        SecurityUtils.checkOwner(f.getSupervisorId());
        if (!SupervisionFeedback.STATUS_COMPLETED.equals(f.getStatus())) {
            throw new BusinessException("仅已完成的反馈可评价");
        }
        if (f.getRating() != null) {
            throw new BusinessException("该反馈已评价，不可重复评价");
        }
        f.setRating(dto.getRating());
        f.setRatingComment(dto.getRatingComment());
        f.setRatingTime(java.time.LocalDateTime.now());
        feedbackService.updateById(f);
        return Result.success("评价成功");
    }

    // ==================== 共享：详情 ====================

    @Operation(summary = "反馈详情（登录可见）")
    @GetMapping("/{id}")
    public Result<SupervisionFeedback> getById(@PathVariable Long id) {
        return Result.ok(feedbackService.getById(id));
    }

    // ==================== 网格员(NEPG)专属：我的指派任务 ====================

    @Operation(summary = "我的指派任务列表（网格员，仅指派给本人的工单）")
    @RequiresRole(Roles.INSPECTOR)
    @GetMapping("/assigned")
    public Result<List<SupervisionFeedback>> assignedToMe(@RequestParam(required = false) String status) {
        LambdaQueryWrapper<SupervisionFeedback> w = new LambdaQueryWrapper<>();
        // 强制作用域：仅指派给当前登录网格员的任务
        w.eq(SupervisionFeedback::getAssignedInspectorId, SecurityUtils.getUserId());
        if (status != null && !status.isEmpty()) {
            w.eq(SupervisionFeedback::getStatus, status);
        }
        // 待检测(ASSIGNED)优先靠前，其次按指派时间倒序
        w.orderByAsc(SupervisionFeedback::getStatus)
                .orderByDesc(SupervisionFeedback::getAssignTime);
        return Result.ok(feedbackService.list(w));
    }

    @Operation(summary = "我的任务统计（网格员：待检测/已完成数量）")
    @RequiresRole(Roles.INSPECTOR)
    @GetMapping("/assigned/stats")
    public Result<Map<String, Object>> assignedStats() {
        Long uid = SecurityUtils.getUserId();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("assigned", countAssigned(uid, SupervisionFeedback.STATUS_ASSIGNED));
        stats.put("completed", countAssigned(uid, SupervisionFeedback.STATUS_COMPLETED));
        stats.put("total", countAssigned(uid, null));
        return Result.ok(stats);
    }

    @Operation(summary = "接受任务（网格员，ASSIGNED→PROCESSING 落库，问题⑦）")
    @RequiresRole(Roles.INSPECTOR)
    @PutMapping("/accept/{id}")
    public Result<Void> accept(@PathVariable Long id) {
        // 归属以登录态为准，仅能接受指派给自己的任务
        feedbackService.accept(id, SecurityUtils.getUserId());
        return Result.success("已接受任务，请尽快前往现场检测");
    }

    @Operation(summary = "网格员工作台概览数据（问题⑥：替换前端静态值）")
    @RequiresRole(Roles.INSPECTOR)
    @GetMapping("/inspector/dashboard")
    public Result<Map<String, Object>> inspectorDashboard() {
        Long uid = SecurityUtils.getUserId();
        Map<String, Object> data = new LinkedHashMap<>();
        // 待检测（含已接受进行中）/ 已完成 / 总数
        data.put("pending", countAssigned(uid, SupervisionFeedback.STATUS_ASSIGNED));
        data.put("processing", countAssigned(uid, SupervisionFeedback.STATUS_PROCESSING));
        data.put("completed", countAssigned(uid, SupervisionFeedback.STATUS_COMPLETED));
        data.put("total", countAssigned(uid, null));
        return Result.ok(data);
    }

    /** 统计指派给某网格员在指定状态下的任务数（status 为 null 表示全部） */
    private long countAssigned(Long inspectorId, String status) {
        LambdaQueryWrapper<SupervisionFeedback> w = new LambdaQueryWrapper<>();
        w.eq(SupervisionFeedback::getAssignedInspectorId, inspectorId);
        if (status != null) {
            w.eq(SupervisionFeedback::getStatus, status);
        }
        return feedbackService.count(w);
    }

    // ==================== 管理员(NEPM)专属：查询与调度 ====================

    @Operation(summary = "分页查询全部反馈（管理员，支持关键词/日期/状态/优先级筛选）")
    @RequiresRole(Roles.ADMIN)
    @GetMapping("/page")
    public PageResult<List<SupervisionFeedback>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LambdaQueryWrapper<SupervisionFeedback> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) w.eq(SupervisionFeedback::getStatus, status);
        if (priority != null && !priority.isEmpty()) w.eq(SupervisionFeedback::getPriority, priority);
        if (keyword != null && !keyword.isEmpty())
            w.and(qw -> qw.like(SupervisionFeedback::getDescription, keyword)
                    .or().like(SupervisionFeedback::getSpecificAddress, keyword));
        if (startDate != null && !startDate.isEmpty())
            w.ge(SupervisionFeedback::getCreateTime, startDate + " 00:00:00");
        if (endDate != null && !endDate.isEmpty())
            w.le(SupervisionFeedback::getCreateTime, endDate + " 23:59:59");
        w.orderByDesc(SupervisionFeedback::getEscalationLevel)
                .orderByAsc(SupervisionFeedback::getEscalatedAt)
                .orderByDesc(SupervisionFeedback::getCreateTime);

        Page<SupervisionFeedback> r = feedbackService.page(new Page<>(page, size), w);
        return PageResult.ok(r.getRecords(), r.getCurrent(), r.getSize(), r.getTotal());
    }

    @Operation(summary = "指派网格员（管理员）")
    @RequiresRole(Roles.ADMIN)
    @PutMapping("/assign/{id}")
    public Result<Void> assign(@PathVariable Long id, @RequestParam Long inspectorId) {
        feedbackService.assign(id, inspectorId);
        return Result.success("指派成功");
    }

    @Operation(summary = "按姓名派送任务（管理员，支持单个/批量反馈，问题①）")
    @RequiresRole(Roles.ADMIN)
    @PostMapping("/assign-by-name")
    public Result<Map<String, Object>> assignByName(@RequestBody Map<String, Object> body) {
        // 入参：feedbackIds(反馈ID数组) + inspectorName(网格员姓名)
        Object rawIds = body.get("feedbackIds");
        String inspectorName = body.get("inspectorName") == null ? null : body.get("inspectorName").toString().trim();
        if (!(rawIds instanceof List<?> idList) || idList.isEmpty()) {
            throw new BusinessException("请选择要派送的反馈工单");
        }
        if (inspectorName == null || inspectorName.isEmpty()) {
            throw new BusinessException("请指定网格员姓名");
        }

        // 经 Feign 调用户服务取网格员列表，做姓名→ID映射与校验
        List<Map<String, Object>> inspectors = userClient.inspectors().getData();
        if (inspectors == null || inspectors.isEmpty()) {
            throw new BusinessException("暂无可用网格员");
        }
        List<Map<String, Object>> matched = inspectors.stream()
                .filter(m -> inspectorName.equals(String.valueOf(m.get("realName"))))
                .toList();
        if (matched.isEmpty()) {
            throw new BusinessException("未找到姓名为【" + inspectorName + "】的可用网格员");
        }
        if (matched.size() > 1) {
            throw new BusinessException("存在多个同名网格员【" + inspectorName + "】，请改用工号或ID派送");
        }
        Long inspectorId = Long.valueOf(matched.get(0).get("id").toString());

        // 复用批量指派逻辑，支持单个或多个反馈
        List<Long> ids = idList.stream().map(o -> Long.valueOf(o.toString())).toList();
        int successCount = feedbackService.batchAssign(ids, inspectorId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inspectorId", inspectorId);
        result.put("inspectorName", inspectorName);
        result.put("successCount", successCount);
        result.put("totalCount", ids.size());
        return Result.ok("按姓名派送完成", result);
    }

    @Operation(summary = "转派工单给另一网格员（管理员）")
    @RequiresRole(Roles.ADMIN)
    @PutMapping("/transfer/{id}")
    public Result<Void> transfer(@PathVariable Long id, @RequestParam Long toInspectorId) {
        feedbackService.transfer(id, toInspectorId);
        return Result.success("转派成功");
    }

    @Operation(summary = "批量指派工单（管理员）")
    @RequiresRole(Roles.ADMIN)
    @PostMapping("/batch-assign")
    public Result<Map<String, Object>> batchAssign(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> rawIds = (List<Object>) body.get("ids");
        if (rawIds == null || rawIds.isEmpty()) {
            throw new BusinessException("请选择要指派的工单");
        }
        List<Long> ids = rawIds.stream().map(o -> Long.valueOf(o.toString())).toList();
        Long inspectorId = Long.valueOf(body.get("inspectorId").toString());
        int successCount = feedbackService.batchAssign(ids, inspectorId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("successCount", successCount);
        result.put("totalCount", ids.size());
        return Result.ok("批量指派完成", result);
    }

    @Operation(summary = "拒绝任务并退回待指派（网格员）")
    @RequiresRole(Roles.INSPECTOR)
    @PutMapping("/reject/{id}")
    public Result<Void> reject(@PathVariable Long id, @RequestParam(required = false) String reason) {
        feedbackService.reject(id, SecurityUtils.getUserId(), reason);
        return Result.success("已拒绝并退回");
    }

    // ==================== 管理员(NEPM)专属：工单升级 & 归档 ====================

    @Operation(summary = "手动升级指定工单（管理员操作）")
    @RequiresRole(Roles.ADMIN)
    @PutMapping("/escalate/{id}")
    public Result<SupervisionFeedback> escalate(@PathVariable Long id) {
        SupervisionFeedback result = feedbackService.escalate(id);
        return Result.ok("工单已升级至 " + result.getEscalationLevel() + " 级（" + result.getPriority() + "）", result);
    }

    @Operation(summary = "获取已升级工单列表（按紧急程度排序）")
    @RequiresRole(Roles.ADMIN)
    @GetMapping("/escalated")
    public Result<List<SupervisionFeedback>> listEscalated() {
        return Result.ok(feedbackService.listEscalated());
    }

    @Operation(summary = "超时工单统计信息")
    @RequiresRole(Roles.ADMIN)
    @GetMapping("/stats")
    public Result<Map<String, Object>> escalationStats() {
        return Result.ok(feedbackService.getEscalationStats());
    }

    @Operation(summary = "反馈全局统计（管理员：总数/各状态数量）")
    @RequiresRole(Roles.ADMIN)
    @GetMapping("/stats/overview")
    public Result<Map<String, Object>> overviewStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", feedbackService.count());
        stats.put("pending", countByStatus(SupervisionFeedback.STATUS_PENDING));
        stats.put("assigned", countByStatus(SupervisionFeedback.STATUS_ASSIGNED));
        stats.put("completed", countByStatus(SupervisionFeedback.STATUS_COMPLETED));
        stats.put("escalated", countByStatus(SupervisionFeedback.STATUS_ESCALATED));
        return Result.ok(stats);
    }

    private long countByStatus(String status) {
        LambdaQueryWrapper<SupervisionFeedback> w = new LambdaQueryWrapper<>();
        w.eq(SupervisionFeedback::getStatus, status);
        return feedbackService.count(w);
    }

    @Operation(summary = "手动触发超时升级扫描（管理员操作，通常由定时任务自动执行）")
    @RequiresRole(Roles.ADMIN)
    @PostMapping("/trigger-escalation")
    public Result<String> triggerEscalation() {
        int count = feedbackService.autoEscalateExpired();
        return Result.ok("升级扫描完成，共升级 " + count + " 个超时工单");
    }

    @Operation(summary = "手动触发冷数据归档（管理员操作）")
    @RequiresRole(Roles.ADMIN)
    @PostMapping("/trigger-archive")
    public Result<String> triggerArchive(@RequestParam(defaultValue = "500") int batchSize) {
        int totalArchived = 0;
        int batchCount;
        do {
            batchCount = feedbackService.autoArchiveCompleted(batchSize);
            totalArchived += batchCount;
        } while (batchCount == batchSize);
        return Result.ok("归档完成，共归档 " + totalArchived + " 条记录");
    }
}
