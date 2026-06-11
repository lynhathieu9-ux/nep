package org.nep.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nep.common.base.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nep_grid_assignment")
public class GridAssignment extends BaseEntity {
    private Long inspectorId;
    private Long provinceId;
    private Long cityId;
    private Integer status;
}
