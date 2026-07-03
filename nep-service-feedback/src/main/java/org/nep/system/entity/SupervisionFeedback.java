package org.nep.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nep.common.base.BaseEntity;

import java.time.LocalDateTime;

/**
 * 公众监督反馈（工单）实体
 *
 * <h3>状态流转</h3>
 * <pre>
 * PENDING ──assign──▶ ASSIGNED ──complete──▶ COMPLETED ──[90d]──▶ ARCHIVED
 *    │                    │
 *    │   [72h 无人处理]    │   [72h 无人处理]
 *    ▼                    ▼
 * ESCALATED ──reassign──▶ ASSIGNED
 *    │
 *    │   [再次72h无人处理，escalationLevel++]
 *    ▼
 * ESCALATED (level 2) ──▶ ESCALATED (level 3, 最高级)
 * </pre>
 *
 * @author NEP Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nep_supervision_feedback")
public class SupervisionFeedback extends BaseEntity {

    // ==================== 基础字段 ====================

    /** 监督员ID（提交者） */
    private Long supervisorId;

    /** 省份ID */
    private Long provinceId;

    /** 城市ID */
    private Long cityId;

    /** 具体地址 */
    private String specificAddress;

    /** 预估AQI等级 (1-6) */
    private Integer estimatedAqiLevel;

    /** 问题描述 */
    private String description;

    /** 状态: PENDING / ASSIGNED / COMPLETED / ESCALATED / ARCHIVED */
    private String status;

    /** 指派的网格员ID */
    private Long assignedInspectorId;

    /** 指派时间 */
    private LocalDateTime assignTime;

    // ==================== 升级 & 催办字段 ⭐ ====================

    /**
     * 升级级别：0=未升级, 1=一级升级(>72h), 2=二级升级(>144h), 3=三级升级(>216h, 最高级)
     * <p>
     * 每次自动扫描触发升级时递增，最高3级。
     * 升级后系统自动发送催办通知。
     */
    private Integer escalationLevel;

    /**
     * 最近一次升级时间
     */
    private LocalDateTime escalatedAt;

    /**
     * 工单截止处理时间（创建/指派时自动计算 = 当前时间 + 72小时）
     */
    private LocalDateTime dueDate;

    /**
     * 优先级: NORMAL / HIGH / URGENT
     * <p>
     * 与 escalationLevel 联动：
     * - escalationLevel 0 → NORMAL
     * - escalationLevel 1 → HIGH
     * - escalationLevel 2+ → URGENT
     */
    private String priority;

    /**
     * 归档时间（COMPLETED 状态超过 90 天后自动归档）
     */
    private LocalDateTime archiveTime;

    /**
     * 是否已归档: 0=未归档, 1=已归档
     */
    private Integer archived;

    // ==================== 监督员满意度评价字段 ====================

    /** 满意度评分（1-5），监督员对已完成反馈的处理质量评价 */
    private Integer rating;

    /** 评价备注 */
    private String ratingComment;

    /** 评价时间 */
    private LocalDateTime ratingTime;

    // ==================== 常量定义 ====================

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ASSIGNED = "ASSIGNED";
    /** 网格员已接受任务、检测进行中（问题⑦：接受任务后落库的中间状态） */
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_ESCALATED = "ESCALATED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    public static final String PRIORITY_NORMAL = "NORMAL";
    public static final String PRIORITY_HIGH = "HIGH";
    public static final String PRIORITY_URGENT = "URGENT";

    /** 超时阈值：72小时（毫秒） */
    public static final long ESCALATION_THRESHOLD_HOURS = 72;

    /** 归档阈值：90天 */
    public static final long ARCHIVE_THRESHOLD_DAYS = 90;

    /** 最大升级级别 */
    public static final int MAX_ESCALATION_LEVEL = 3;
}
