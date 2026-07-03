package org.nep.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 监督员提交空气质量监督反馈 - 入参 DTO。
 * <p>仅承载监督员可填写的字段；supervisorId 由后端从登录态取得，
 * 不接受前端传入，杜绝冒名提交。所有字段服务端强校验。</p>
 *
 * @author NEP Team
 */
@Data
public class FeedbackSubmitDTO {

    /** 省份ID（网格区域-省） */
    @NotNull(message = "请选择省份")
    private Long provinceId;

    /** 城市ID（网格区域-市） */
    @NotNull(message = "请选择城市")
    private Long cityId;

    /** 本人观测的具体地址 */
    @NotBlank(message = "请填写具体观测地址")
    @Size(max = 200, message = "具体地址不能超过200字")
    private String specificAddress;

    /**
     * 预估 AQI 等级（1-6，对照“空气质量指数范围及类别表”）：
     * 1优 2良 3轻度污染 4中度污染 5重度污染 6严重污染
     */
    @NotNull(message = "请预估AQI等级")
    @Min(value = 1, message = "AQI等级最小为1（优）")
    @Max(value = 6, message = "AQI等级最大为6（严重污染）")
    private Integer estimatedAqiLevel;

    /** 观测到的空气质量描述信息 */
    @NotBlank(message = "请填写空气质量描述")
    @Size(max = 500, message = "描述不能超过500字")
    private String description;
}
