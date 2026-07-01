package org.nep.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nep.common.base.BaseEntity;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nep_supervision_feedback")
public class SupervisionFeedback extends BaseEntity {
    private Long supervisorId;
    private Long provinceId;
    private Long cityId;
    private String specificAddress;
    /** 经度 (地图选点) */
    private Double longitude;
    /** 纬度 (地图选点) */
    private Double latitude;
    private Integer estimatedAqiLevel;
    private String description;
    /** 状态: PENDING/ASSIGNED/COMPLETED/REJECTED/ESCALATED */
    private String status;
    private Long assignedInspectorId;
    private LocalDateTime assignTime;
    /** 拒绝原因 (REJECTED时填写) */
    private String rejectReason;
    /** 满意度评分 1-5 */
    private Integer rating;
    /** 评价备注 */
    private String ratingComment;
    /** 评价时间 */
    private LocalDateTime ratingTime;
}
