package org.nep.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nep.common.base.BaseEntity;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nep_aqi_detection")
public class AqiDetection extends BaseEntity {
    private Long feedbackId;
    private Long inspectorId;
    private Long provinceId;
    private Long cityId;
    private Integer so2Aqi;
    private Integer coAqi;
    private Integer pm25Aqi;
    private Integer finalAqi;
    private LocalDateTime detectionTime;
    private String remark;
}
