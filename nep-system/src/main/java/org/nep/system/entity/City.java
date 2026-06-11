package org.nep.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nep.common.base.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nep_city")
public class City extends BaseEntity {
    private String name;
    private String code;
    private Long provinceId;
    private String cityLevel;
    private Integer isCapital;
    private Integer sortOrder;
}
