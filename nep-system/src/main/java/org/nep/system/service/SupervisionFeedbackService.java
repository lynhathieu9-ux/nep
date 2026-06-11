package org.nep.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.nep.system.entity.SupervisionFeedback;

public interface SupervisionFeedbackService extends IService<SupervisionFeedback> {
    SupervisionFeedback submit(SupervisionFeedback feedback);
    void assign(Long feedbackId, Long inspectorId);
}
