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
    private Integer estimatedAqiLevel;
    private String description;
    private String status;
    private Long assignedInspectorId;
    private LocalDateTime assignTime;
}
