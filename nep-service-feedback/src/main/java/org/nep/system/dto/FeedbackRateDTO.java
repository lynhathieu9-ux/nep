package org.nep.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 监督员对已完成反馈的满意度评价 - 入参 DTO。
 *
 * @author NEP Team
 */
@Data
public class FeedbackRateDTO {

    /** 满意度评分（1-5星） */
    @NotNull(message = "请选择评分")
    @Min(value = 1, message = "评分最低1星")
    @Max(value = 5, message = "评分最高5星")
    private Integer rating;

    /** 评价备注（可选） */
    @Size(max = 500, message = "评价备注不能超过500字")
    private String ratingComment;
}
