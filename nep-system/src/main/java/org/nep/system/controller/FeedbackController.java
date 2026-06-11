package org.nep.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nep.common.result.PageResult;
import org.nep.common.result.Result;
import org.nep.system.entity.SupervisionFeedback;
import org.nep.system.service.SupervisionFeedbackService;
import org.springframework.web.bind.annotation.*;

@Tag(name = "监督反馈管理")
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final SupervisionFeedbackService feedbackService;
    public FeedbackController(SupervisionFeedbackService s) { this.feedbackService = s; }

    @Operation(summary = "提交监督反馈")
    @PostMapping("/submit")
    public Result<SupervisionFeedback> submit(@RequestBody SupervisionFeedback f) {
        return Result.ok("反馈提交成功", feedbackService.submit(f));
    }

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public PageResult<java.util.List<SupervisionFeedback>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<SupervisionFeedback> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) w.eq(SupervisionFeedback::getStatus, status);
        w.orderByDesc(SupervisionFeedback::getCreateTime);
        Page<SupervisionFeedback> r = feedbackService.page(new Page<>(page, size), w);
        return PageResult.ok(r.getRecords(), r.getCurrent(), r.getSize(), r.getTotal());
    }

    @Operation(summary = "详情")
    @GetMapping("/{id}")
    public Result<SupervisionFeedback> getById(@PathVariable Long id) { return Result.ok(feedbackService.getById(id)); }

    @Operation(summary = "指派网格员")
    @PutMapping("/assign/{id}")
    public Result<Void> assign(@PathVariable Long id, @RequestParam Long inspectorId) {
        feedbackService.assign(id, inspectorId); return Result.success("指派成功");
    }

    @Operation(summary = "我的反馈")
    @GetMapping("/my/{supervisorId}")
    public Result<java.util.List<SupervisionFeedback>> my(@PathVariable Long supervisorId) {
        LambdaQueryWrapper<SupervisionFeedback> w = new LambdaQueryWrapper<>();
        w.eq(SupervisionFeedback::getSupervisorId, supervisorId).orderByDesc(SupervisionFeedback::getCreateTime);
        return Result.ok(feedbackService.list(w));
    }
}
