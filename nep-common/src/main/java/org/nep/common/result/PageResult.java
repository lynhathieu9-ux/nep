package org.nep.common.result;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PageResult<T> extends Result<T> {
    private Long page;
    private Long size;
    private Long total;
    private Long pages;

    public static <T> PageResult<T> ok(T data, Long page, Long size, Long total) {
        PageResult<T> r = new PageResult<>();
        r.setCode(200); r.setMessage("操作成功"); r.setData(data);
        r.setPage(page); r.setSize(size); r.setTotal(total);
        r.setPages(total % size == 0 ? total / size : total / size + 1);
        return r;
    }
}
