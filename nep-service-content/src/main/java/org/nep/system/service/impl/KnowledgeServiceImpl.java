package org.nep.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.nep.system.entity.Knowledge;
import org.nep.system.mapper.KnowledgeMapper;
import org.nep.system.service.KnowledgeService;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeMapper, Knowledge> implements KnowledgeService {

    /**
     * 原子自增浏览次数。
     * 使用 UPDATE view_count = view_count + 1 防并发丢失，不走 read-then-write。
     */
    @Override
    public void incrementViewCount(Long id) {
        baseMapper.incrementViewCount(id);
    }

    /**
     * 原子自增点赞数。
     */
    @Override
    public void incrementLikeCount(Long id) {
        baseMapper.incrementLikeCount(id);
    }
}
