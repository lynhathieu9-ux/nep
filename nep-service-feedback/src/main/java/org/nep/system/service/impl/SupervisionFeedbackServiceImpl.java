package org.nep.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nep.common.exception.BusinessException;
import org.nep.system.entity.SupervisionFeedback;
import org.nep.system.mapper.SupervisionFeedbackMapper;
import org.nep.system.service.SupervisionFeedbackService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 监督反馈服务实现
 *
 * <h3>核心业务逻辑</h3>
 * <ul>
 *   <li>反馈提交 → PENDING 状态，自动计算 dueDate = 当前时间 + 72h</li>
 *   <li>指派网格员 → ASSIGNED 状态，刷新 dueDate</li>
 *   <li>超时升级 → PENDING/ASSIGNED/ESCALATED > 72h → escalationLevel++</li>
 *   <li>冷数据归档 → COMPLETED > 90d → archived = 1</li>
 * </ul>
 *
 * @author NEP Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupervisionFeedbackServiceImpl
        extends ServiceImpl<SupervisionFeedbackMapper, SupervisionFeedback>
        implements SupervisionFeedbackService {

    private final SupervisionFeedbackMapper feedbackMapper;

    // ==================== 基础业务 ====================

    @Override
    @Transactional
    public SupervisionFeedback submit(SupervisionFeedback feedback) {
        feedback.setStatus(SupervisionFeedback.STATUS_PENDING);
        feedback.setEscalationLevel(0);
        feedback.setPriority(SupervisionFeedback.PRIORITY_NORMAL);
        feedback.setDueDate(LocalDateTime.now().plusHours(SupervisionFeedback.ESCALATION_THRESHOLD_HOURS));
        feedback.setArchived(0);
        this.save(feedback);
        log.info("反馈提交成功: id={}, dueDate={}", feedback.getId(), feedback.getDueDate());
        return feedback;
    }

    @Override
    @Transactional
    public void assign(Long feedbackId, Long inspectorId) {
        SupervisionFeedback f = this.getById(feedbackId);
        if (f == null) throw new BusinessException("反馈记录不存在");
        if (!SupervisionFeedback.STATUS_PENDING.equals(f.getStatus())
                && !SupervisionFeedback.STATUS_ESCALATED.equals(f.getStatus())) {
            throw new BusinessException("该反馈状态不允许指派（当前：" + f.getStatus() + "）");
        }

        f.setAssignedInspectorId(inspectorId);
        f.setAssignTime(LocalDateTime.now());
        f.setStatus(SupervisionFeedback.STATUS_ASSIGNED);
        // 指派后重置截止时间
        f.setDueDate(LocalDateTime.now().plusHours(SupervisionFeedback.ESCALATION_THRESHOLD_HOURS));
        // 如果是升级后重新指派，保留 escalationLevel 但重置 priority
        if (f.getEscalationLevel() > 0) {
            f.setPriority(SupervisionFeedback.PRIORITY_HIGH);
        }
        this.updateById(f);
        log.info("反馈指派成功: id={}, inspectorId={}, dueDate={}", feedbackId, inspectorId, f.getDueDate());
    }

    @Override
    @Transactional
    public void transfer(Long feedbackId, Long toInspectorId) {
        SupervisionFeedback f = this.getById(feedbackId);
        if (f == null) throw new BusinessException("反馈记录不存在");
        if (!SupervisionFeedback.STATUS_ASSIGNED.equals(f.getStatus())) {
            throw new BusinessException("仅已指派状态的工单可转派（当前：" + f.getStatus() + "）");
        }
        if (toInspectorId.equals(f.getAssignedInspectorId())) {
            throw new BusinessException("新任网格员不能与原网格员相同");
        }
        f.setAssignedInspectorId(toInspectorId);
        f.setAssignTime(LocalDateTime.now());
        // 转派刷新截止时间，给新网格员完整处理窗口
        f.setDueDate(LocalDateTime.now().plusHours(SupervisionFeedback.ESCALATION_THRESHOLD_HOURS));
        this.updateById(f);
        log.info("工单转派成功: id={}, newInspectorId={}", feedbackId, toInspectorId);
    }

    @Override
    @Transactional
    public int batchAssign(List<Long> feedbackIds, Long inspectorId) {
        if (feedbackIds == null || feedbackIds.isEmpty()) {
            throw new BusinessException("请选择要指派的工单");
        }
        int success = 0;
        for (Long id : feedbackIds) {
            try {
                assign(id, inspectorId);
                success++;
            } catch (Exception e) {
                // 单条失败不影响其余，记录后继续
                log.warn("批量指派跳过工单 {}: {}", id, e.getMessage());
            }
        }
        log.info("批量指派完成: 成功 {}/{}, inspectorId={}", success, feedbackIds.size(), inspectorId);
        return success;
    }

    @Override
    @Transactional
    public void reject(Long feedbackId, Long inspectorId, String reason) {
        SupervisionFeedback f = this.getById(feedbackId);
        if (f == null) throw new BusinessException("反馈记录不存在");
        if (!SupervisionFeedback.STATUS_ASSIGNED.equals(f.getStatus())) {
            throw new BusinessException("仅已指派状态的工单可拒绝");
        }
        // 归属校验：只能拒绝指派给自己的任务
        if (f.getAssignedInspectorId() == null || !f.getAssignedInspectorId().equals(inspectorId)) {
            throw new BusinessException(403, "该任务未指派给你，无法拒绝");
        }
        // 退回待指派，清空指派信息，待管理员重新指派
        f.setStatus(SupervisionFeedback.STATUS_PENDING);
        f.setAssignedInspectorId(null);
        f.setAssignTime(null);
        this.updateById(f);
        log.info("工单被拒绝退回: id={}, byInspector={}, reason={}", feedbackId, inspectorId, reason);
    }

    /**
     * 问题⑦：网格员接受任务。仅本人被指派的 ASSIGNED 工单可接受，
     * 状态置为 PROCESSING 并落库，修复"接受操作后数据库状态未同步"的 bug。
     */
    @Override
    @Transactional
    public void accept(Long feedbackId, Long inspectorId) {
        SupervisionFeedback f = this.getById(feedbackId);
        if (f == null) throw new BusinessException("反馈记录不存在");
        // 归属校验：只能接受指派给自己的任务
        if (f.getAssignedInspectorId() == null || !f.getAssignedInspectorId().equals(inspectorId)) {
            throw new BusinessException(403, "该任务未指派给你，无法接受");
        }
        if (SupervisionFeedback.STATUS_PROCESSING.equals(f.getStatus())) {
            throw new BusinessException("该任务已接受，无需重复操作");
        }
        if (!SupervisionFeedback.STATUS_ASSIGNED.equals(f.getStatus())) {
            throw new BusinessException(400, "仅待检测(已指派)的任务可接受");
        }
        f.setStatus(SupervisionFeedback.STATUS_PROCESSING);
        this.updateById(f);
        log.info("网格员接受任务: id={}, inspectorId={}, 状态→PROCESSING", feedbackId, inspectorId);
    }

    @Override
    @Transactional
    public void cancel(Long feedbackId, Long supervisorId) {
        SupervisionFeedback f = this.getById(feedbackId);
        if (f == null) throw new BusinessException("反馈记录不存在");
        if (!f.getSupervisorId().equals(supervisorId)) throw new BusinessException("只能撤回自己的反馈");
        if (!SupervisionFeedback.STATUS_PENDING.equals(f.getStatus()))
            throw new BusinessException("只能撤回待指派状态的反馈");
        this.removeById(feedbackId);
        log.info("反馈已撤回: id={}", feedbackId);
    }

    // ==================== ⭐ 工单升级 ====================

    @Override
    @Transactional
    public SupervisionFeedback escalate(Long feedbackId) {
        SupervisionFeedback f = this.getById(feedbackId);
        if (f == null) throw new BusinessException("反馈记录不存在");

        int newLevel = Math.min(f.getEscalationLevel() + 1, SupervisionFeedback.MAX_ESCALATION_LEVEL);
        String newPriority = switch (newLevel) {
            case 1 -> SupervisionFeedback.PRIORITY_HIGH;
            case 2, 3 -> SupervisionFeedback.PRIORITY_URGENT;
            default -> SupervisionFeedback.PRIORITY_NORMAL;
        };

        f.setStatus(SupervisionFeedback.STATUS_ESCALATED);
        f.setEscalationLevel(newLevel);
        f.setPriority(newPriority);
        f.setEscalatedAt(LocalDateTime.now());
        f.setDueDate(LocalDateTime.now().plusHours(SupervisionFeedback.ESCALATION_THRESHOLD_HOURS));
        this.updateById(f);

        log.warn("工单已升级: id={}, level={}, priority={}, reason=超过{}小时未处理",
                feedbackId, newLevel, newPriority, SupervisionFeedback.ESCALATION_THRESHOLD_HOURS);
        return f;
    }

    @Override
    public int autoEscalateExpired() {
        List<SupervisionFeedback> overdueList = feedbackMapper.selectOverdueForEscalation(
                (int) SupervisionFeedback.ESCALATION_THRESHOLD_HOURS,
                SupervisionFeedback.MAX_ESCALATION_LEVEL);

        if (overdueList.isEmpty()) {
            log.info("自动扫描：无超时工单需要升级");
            return 0;
        }

        int count = 0;
        for (SupervisionFeedback f : overdueList) {
            try {
                escalate(f.getId());
                count++;
            } catch (Exception e) {
                log.error("自动升级工单 {} 失败: {}", f.getId(), e.getMessage());
            }
        }

        log.info("自动升级完成: {} 个超时工单已升级", count);
        return count;
    }

    @Override
    @Transactional
    public int autoArchiveCompleted(int batchSize) {
        List<SupervisionFeedback> toArchive = feedbackMapper.selectCompletedForArchive(
                (int) SupervisionFeedback.ARCHIVE_THRESHOLD_DAYS, batchSize);

        if (toArchive.isEmpty()) {
            log.info("自动归档：无待归档数据");
            return 0;
        }

        List<Long> ids = toArchive.stream().map(SupervisionFeedback::getId).toList();
        int count = feedbackMapper.batchMarkArchived(ids);

        log.info("自动归档完成: {} 条 COMPLETED 数据已归档（>{}天）",
                count, SupervisionFeedback.ARCHIVE_THRESHOLD_DAYS);
        return count;
    }

    @Override
    public Map<String, Object> getEscalationStats() {
        List<Map<String, Object>> statusCounts = feedbackMapper.countByStatus();

        Map<String, Object> stats = new LinkedHashMap<>();
        // 按状态统计
        Map<String, Long> statusMap = new LinkedHashMap<>();
        for (Map<String, Object> row : statusCounts) {
            statusMap.put(String.valueOf(row.get("status")),
                    ((Number) row.get("cnt")).longValue());
        }
        stats.put("byStatus", statusMap);

        // 超时统计
        long overdueCount = feedbackMapper.countOverdue(
                (int) SupervisionFeedback.ESCALATION_THRESHOLD_HOURS);
        stats.put("overdueCount", overdueCount);
        stats.put("overdueThresholdHours", SupervisionFeedback.ESCALATION_THRESHOLD_HOURS);
        stats.put("escalationMaxLevel", SupervisionFeedback.MAX_ESCALATION_LEVEL);

        // 升级级别分布
        List<Map<String, Object>> levelDist = new ArrayList<>();
        for (int i = 0; i <= SupervisionFeedback.MAX_ESCALATION_LEVEL; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("level", i);
            String label = switch (i) {
                case 0 -> "正常";
                case 1 -> "一级升级(>72h)";
                case 2 -> "二级升级(>144h)";
                case 3 -> "三级升级(>216h, 最高)";
                default -> "未知";
            };
            m.put("label", label);
            levelDist.add(m);
        }
        stats.put("escalationLevels", levelDist);

        return stats;
    }

    @Override
    public List<SupervisionFeedback> listEscalated() {
        return feedbackMapper.selectEscalated();
    }
}
