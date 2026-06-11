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
        SupervisionFeedback feedback = feedbackMapper.selectById(detection.getFeedbackId());
        if (feedback == null) throw new BusinessException("关联的反馈记录不存在");
        if (!"ASSIGNED".equals(feedback.getStatus())) throw new BusinessException("反馈记录状态不正确");

        int finalAqi = Math.max(Math.max(detection.getSo2Aqi(), detection.getCoAqi()), detection.getPm25Aqi());
        detection.setFinalAqi(finalAqi);
        detection.setDetectionTime(LocalDateTime.now());
        this.save(detection);

        feedback.setStatus("COMPLETED");
        feedbackMapper.updateById(feedback);
        return detection;
    }
}
