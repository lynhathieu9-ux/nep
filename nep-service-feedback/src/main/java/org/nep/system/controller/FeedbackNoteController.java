package org.nep.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nep.common.annotation.RequiresRole;
import org.nep.common.constant.Roles;
import org.nep.common.result.Result;
import org.nep.common.utils.SecurityUtils;
import org.nep.system.entity.FeedbackNote;
import org.nep.system.service.FeedbackNoteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 反馈内部备注控制器。
 * <p>网格员现场记录、管理员批注共用；添加备注限网格员/管理员并以登录态记录操作人，查询登录可见。</p>
 *
 * @author NEP Team
 */
@Tag(name = "反馈内部备注")
@RestController
@RequestMapping("/api/feedback-note")
public class FeedbackNoteController {

    private final FeedbackNoteService noteService;

    public FeedbackNoteController(FeedbackNoteService s) {
        this.noteService = s;
    }

    @Operation(summary = "添加内部备注（网格员现场记录 / 管理员批注）")
    @RequiresRole({Roles.INSPECTOR, Roles.ADMIN})
    @PostMapping
    public Result<FeedbackNote> add(@RequestBody FeedbackNote note) {
        // 操作人以登录态为准，防伪造
        note.setUserId(SecurityUtils.getUserId());
        noteService.save(note);
        return Result.ok("备注添加成功", note);
    }

    @Operation(summary = "查询某反馈的所有备注")
    @GetMapping("/feedback/{feedbackId}")
    public Result<List<FeedbackNote>> listByFeedback(@PathVariable Long feedbackId) {
        LambdaQueryWrapper<FeedbackNote> w = new LambdaQueryWrapper<>();
        w.eq(FeedbackNote::getFeedbackId, feedbackId).orderByAsc(FeedbackNote::getCreateTime);
        return Result.ok(noteService.list(w));
    }

    @Operation(summary = "删除备注（管理员）")
    @RequiresRole(Roles.ADMIN)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noteService.removeById(id);
        return Result.success("备注已删除");
    }
}
