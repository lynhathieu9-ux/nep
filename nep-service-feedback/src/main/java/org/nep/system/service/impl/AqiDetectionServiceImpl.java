package org.nep.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.nep.common.exception.BusinessException;
import org.nep.system.entity.AqiDetection;
import org.nep.system.entity.SupervisionFeedback;
import org.nep.system.mapper.AqiDetectionMapper;
import org.nep.system.mapper.SupervisionFeedbackMapper;
import org.nep.system.service.AqiDetectionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class AqiDetectionServiceImpl
        extends ServiceImpl<AqiDetectionMapper, AqiDetection>
        implements AqiDetectionService {

    private final SupervisionFeedbackMapper feedbackMapper;

    public AqiDetectionServiceImpl(SupervisionFeedbackMapper feedbackMapper) {
        this.feedbackMapper = feedbackMapper;
    }

    @Override
    @Transactional
    public AqiDetection submitDetection(AqiDetection detection) {
        // 数值防御校验（DTO 已校验，此处再兜底，防止service被其它入口调用时越界）
        checkRange(detection.getSo2Aqi(), "SO2");
        checkRange(detection.getCoAqi(), "CO");
        checkRange(detection.getPm25Aqi(), "PM2.5");

        SupervisionFeedback feedback = feedbackMapper.selectById(detection.getFeedbackId());
        if (feedback == null) throw new BusinessException(404, "关联的反馈记录不存在");
        // 先做归属校验：任务必须指派给当前提交的网格员本人（防越权，返回403）
        if (feedback.getAssignedInspectorId() == null
                || !feedback.getAssignedInspectorId().equals(detection.getInspectorId())) {
            throw new BusinessException(403, "该检测任务未指派给你，无法提交");
        }
        // 再做状态校验：待检测(已指派)或进行中(已接受)均可提交检测（问题⑦联动）
        if (!SupervisionFeedback.STATUS_ASSIGNED.equals(feedback.getStatus())
                && !SupervisionFeedback.STATUS_PROCESSING.equals(feedback.getStatus())) {
            throw new BusinessException(400, "该任务当前状态不可提交检测（需为已指派或进行中）");
        }

        // 检测位置继承自反馈工单，保证省/市统计口径一致
        detection.setProvinceId(feedback.getProvinceId());
        detection.setCityId(feedback.getCityId());
        // 最终 AQI = MAX(SO2, CO, PM2.5)，符合需求书 AQI 计算规则
        int finalAqi = Math.max(Math.max(detection.getSo2Aqi(), detection.getCoAqi()), detection.getPm25Aqi());
        detection.setFinalAqi(finalAqi);
        detection.setDetectionTime(LocalDateTime.now());
        this.save(detection);

        // 回写工单为已完成，形成"指派→检测→完成"闭环
        feedback.setStatus(SupervisionFeedback.STATUS_COMPLETED);
        feedbackMapper.updateById(feedback);
        return detection;
    }

    /** 校验单项 AQI 分指数是否在 0~500 合法区间 */
    private void checkRange(Integer value, String name) {
        if (value == null) {
            throw new BusinessException(name + " 检测值不能为空");
        }
        if (value < 0 || value > 500) {
            throw new BusinessException(name + " AQI 分指数应在 0~500 之间");
        }
    }
}
