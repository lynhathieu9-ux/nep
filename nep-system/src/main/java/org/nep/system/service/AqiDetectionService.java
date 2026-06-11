package org.nep.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.nep.system.entity.AqiDetection;

public interface AqiDetectionService extends IService<AqiDetection> {
    AqiDetection submitDetection(AqiDetection detection);
}
