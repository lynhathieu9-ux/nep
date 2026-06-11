package org.nep.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.nep.common.exception.BusinessException;
import org.nep.system.entity.SupervisionFeedback;
import org.nep.system.mapper.SupervisionFeedbackMapper;
import org.nep.system.service.SupervisionFeedbackService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class SupervisionFeedbackServiceImpl
        extends ServiceImpl<SupervisionFeedbackMapper, SupervisionFeedback>
        implements SupervisionFeedbackService {

    @Override
    public SupervisionFeedback submit(SupervisionFeedback feedback) {
        feedback.setStatus("PENDING");
        this.save(feedback);
        return feedback;
    }

    @Override
    @Transactional
    public void assign(Long feedbackId, Long inspectorId) {
        SupervisionFeedback f = this.getById(feedbackId);
        if (f == null) throw new BusinessException("反馈记录不存在");
        if (!"PENDING".equals(f.getStatus())) throw new BusinessException("该反馈已指派或已完成");

        f.setAssignedInspectorId(inspectorId);
        f.setAssignTime(LocalDateTime.now());
        f.setStatus("ASSIGNED");
        this.updateById(f);
    }
}
