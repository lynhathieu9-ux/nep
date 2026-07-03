package org.nep.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nep.common.annotation.RequiresRole;
import org.nep.common.constant.Roles;
import org.nep.common.exception.BusinessException;
import org.nep.common.result.Result;
import org.nep.feedback.client.UserClient;
import org.nep.system.entity.GridAssignment;
import org.nep.system.entity.SupervisionFeedback;
import org.nep.system.entity.User;
import org.nep.system.mapper.GridAssignmentMapper;
import org.nep.system.service.SupervisionFeedbackService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 网格员智能指派控制器（管理员专属）。
 * <p>实现需求书"本地优先、就近异地"的指派推荐：
 * <ol>
 *   <li>本地指派：反馈所在城市有可工作网格员 → 优先推荐</li>
 *   <li>异地指派：本地无可用网格员 → 推荐同省其他城市的网格员（就近）</li>
 *   <li>全局兜底：同省也无 → 推荐全国任意可工作网格员</li>
 * </ol>
 * 通过 OpenFeign 调用用户微服务补全网格员姓名/联系方式，体现分布式服务协作。</p>
 *
 * @author NEP Team
 */
@Tag(name = "网格员智能指派")
@RestController
@RequestMapping("/api/assignment")
public class AssignmentController {

    private final GridAssignmentMapper gridMapper;
    private final SupervisionFeedbackService feedbackService;
    private final UserClient userClient;

    public AssignmentController(GridAssignmentMapper gridMapper,
                                SupervisionFeedbackService feedbackService,
                                UserClient userClient) {
        this.gridMapper = gridMapper;
        this.feedbackService = feedbackService;
        this.userClient = userClient;
    }

    @Operation(summary = "为反馈推荐网格员（本地优先→就近异地→全局，管理员）")
    @RequiresRole(Roles.ADMIN)
    @GetMapping("/recommend/{feedbackId}")
    public Result<Map<String, Object>> recommend(@PathVariable Long feedbackId) {
        SupervisionFeedback feedback = feedbackService.getById(feedbackId);
        if (feedback == null) {
            throw new BusinessException("反馈记录不存在");
        }

        // 1) 本地：同城市可工作网格员
        List<GridAssignment> local = queryAvailable(feedback.getProvinceId(), feedback.getCityId(), true);
        String assignType;
        List<GridAssignment> candidates;
        if (!local.isEmpty()) {
            assignType = "LOCAL";
            candidates = local;
        } else {
            // 2) 异地就近：同省其他城市可工作网格员
            List<GridAssignment> province = queryAvailable(feedback.getProvinceId(), null, true);
            if (!province.isEmpty()) {
                assignType = "REMOTE_PROVINCE";
                candidates = province;
            } else {
                // 3) 全局兜底：任意可工作网格员
                assignType = "REMOTE_GLOBAL";
                candidates = queryAvailable(null, null, true);
            }
        }

        // 组装推荐结果，通过 Feign 补全网格员信息
        List<Map<String, Object>> inspectors = new ArrayList<>();
        for (GridAssignment g : candidates) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("inspectorId", g.getInspectorId());
            item.put("provinceId", g.getProvinceId());
            item.put("cityId", g.getCityId());
            try {
                Result<User> ur = userClient.getById(g.getInspectorId());
                if (ur != null && ur.getData() != null) {
                    item.put("realName", ur.getData().getRealName());
                    item.put("phone", ur.getData().getPhone());
                    item.put("employeeCode", ur.getData().getEmployeeCode());
                }
            } catch (Exception e) {
                item.put("realName", "网格员" + g.getInspectorId());
            }
            inspectors.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assignType", assignType);
        result.put("assignTypeText", switch (assignType) {
            case "LOCAL" -> "本地指派";
            case "REMOTE_PROVINCE" -> "就近异地指派（同省）";
            default -> "跨省异地指派";
        });
        result.put("inspectors", inspectors);
        return Result.ok(result);
    }

    /**
     * 查询可工作网格员的区域分配记录。
     *
     * @param provinceId 省份ID，null 表示不限
     * @param cityId     城市ID，null 表示不限
     * @param onlyActive 是否仅查可工作状态(status=1)
     */
    private List<GridAssignment> queryAvailable(Long provinceId, Long cityId, boolean onlyActive) {
        LambdaQueryWrapper<GridAssignment> w = new LambdaQueryWrapper<>();
        if (provinceId != null) w.eq(GridAssignment::getProvinceId, provinceId);
        if (cityId != null) w.eq(GridAssignment::getCityId, cityId);
        if (onlyActive) w.eq(GridAssignment::getStatus, 1);
        return gridMapper.selectList(w);
    }
}
