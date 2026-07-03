package org.nep.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 网格员提交实测 AQI 数据 - 入参 DTO。
 * <p>三项污染物 AQI 分指数均须在 0~500 合法区间；inspectorId 由后端从登录态取得。
 * 最终 AQI 由后端按 AQI = MAX(SO2, CO, PM2.5) 计算，不接受前端传入，保证口径统一。</p>
 *
 * @author NEP Team
 */
@Data
public class AqiSubmitDTO {

    /** 关联的监督反馈（工单）ID */
    @NotNull(message = "缺少关联的检测任务")
    private Long feedbackId;

    /** SO2 二氧化硫 AQI 分指数（0-500） */
    @NotNull(message = "请填写SO2检测值")
    @Min(value = 0, message = "SO2 AQI 不能小于0")
    @Max(value = 500, message = "SO2 AQI 不能大于500")
    private Integer so2Aqi;

    /** CO 一氧化碳 AQI 分指数（0-500） */
    @NotNull(message = "请填写CO检测值")
    @Min(value = 0, message = "CO AQI 不能小于0")
    @Max(value = 500, message = "CO AQI 不能大于500")
    private Integer coAqi;

    /** PM2.5 悬浮颗粒物 AQI 分指数（0-500） */
    @NotNull(message = "请填写PM2.5检测值")
    @Min(value = 0, message = "PM2.5 AQI 不能小于0")
    @Max(value = 500, message = "PM2.5 AQI 不能大于500")
    private Integer pm25Aqi;

    /** 检测备注（可选） */
    @Size(max = 500, message = "备注不能超过500字")
    private String remark;
}
