package org.nep.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.nep.system.entity.SupervisionFeedback;

import java.util.List;
import java.util.Map;

/**
 * 监督反馈服务接口
 *
 * @author NEP Team
 */
public interface SupervisionFeedbackService extends IService<SupervisionFeedback> {

    /** 提交反馈 */
    SupervisionFeedback submit(SupervisionFeedback feedback);

    /** 指派网格员 */
    void assign(Long feedbackId, Long inspectorId);

    /** 转派：将已指派工单改派给另一网格员（管理员） */
    void transfer(Long feedbackId, Long toInspectorId);

    /** 批量指派：把多个待指派工单一次性指派给同一网格员，返回成功数量 */
    int batchAssign(java.util.List<Long> feedbackIds, Long inspectorId);

    /** 网格员拒绝任务：退回为待指派并记录原因（管理员可重新指派） */
    void reject(Long feedbackId, Long inspectorId, String reason);

    /** 网格员接受任务：ASSIGNED→PROCESSING 落库（问题⑦），带归属校验 */
    void accept(Long feedbackId, Long inspectorId);

    /** 撤回反馈（仅PENDING状态） */
    void cancel(Long feedbackId, Long supervisorId);

    // ==================== ⭐ 工单升级 & 归档 ====================

    /**
     * 手动升级指定工单
     * @param feedbackId 工单ID
     * @return 升级后的工单（含新 escalationLevel 和 priority）
     */
    SupervisionFeedback escalate(Long feedbackId);

    /**
     * 自动扫描并升级所有超时工单（定时任务调用）
     * <p>
     * 升级规则：
     * <ul>
     *   <li>PENDING/ASSIGNED 状态 >72h → 升级1级</li>
     *   <li>ESCALATED 状态再次 >72h → 继续升级（最高3级）</li>
     *   <li>升级级别 < 3 时持续扫描</li>
     * </ul>
     * @return 本次升级的工单数量
     */
    int autoEscalateExpired();

    /**
     * 自动归档已完成的冷数据（定时任务调用）
     * <p>
     * 归档规则：COMPLETED 状态 >90 天的工单标记为归档
     *
     * @param batchSize 每批处理数量（防大事务）
     * @return 本次归档数量
     */
    int autoArchiveCompleted(int batchSize);

    /**
     * 获取超时工单的统计信息
     */
    Map<String, Object> getEscalationStats();

    /**
     * 获取所有已升级的工单列表
     */
    List<SupervisionFeedback> listEscalated();
}
