package org.nep.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.nep.system.entity.Knowledge;

@Mapper
public interface KnowledgeMapper extends BaseMapper<Knowledge> {

    /** 原子自增浏览次数（防并发丢失） */
    @Update("UPDATE nep_knowledge SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(Long id);

    /** 原子自增点赞数 */
    @Update("UPDATE nep_knowledge SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(Long id);
}
