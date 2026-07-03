package org.nep.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nep.common.base.BaseEntity;

/**
 * 环保知识库实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nep_knowledge")
public class Knowledge extends BaseEntity {
    /** 知识标题 */
    private String title;
    /** 知识摘要 */
    private String summary;
    /** 知识内容（富文本） */
    private String content;
    /** 封面图片URL */
    private String coverImage;
    /** 分类: AIR-大气, WATER-水, SOIL-土壤, NOISE-噪声, ECOLOGY-生态 */
    private String category;
    /** 标签（JSON数组） */
    private String tags;
    /** 来源 */
    private String source;
    /** 浏览次数 */
    private Long viewCount;
    /** 点赞数 */
    private Long likeCount;
    /** 状态: 0-草稿, 1-已发布 */
    private Integer status;
    /** 发布人ID */
    private Long publisherId;
    /** 附件文件URL（问题⑤：知识库文件下载） */
    private String attachmentUrl;
}
