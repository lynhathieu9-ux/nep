package org.nep.common.result;

import lombok.Data;
import java.io.Serializable;

@Data
public class Result<T> implements Serializable {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    protected Result() { this.timestamp = System.currentTimeMillis(); }

    public static <T> Result<T> ok() { return ok(null); }
    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.setCode(200); r.setMessage("操作成功"); r.setData(data);
        return r;
    }
    public static <T> Result<T> ok(String message, T data) {
        Result<T> r = ok(data); r.setMessage(message); return r;
    }

    public static <T> Result<T> fail() { return fail(500, "操作失败"); }
    public static <T> Result<T> fail(String message) { return fail(500, message); }
    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code); r.setMessage(message); return r;
    }

    /** 仅返回成功消息（无数据） */
    public static <T> Result<T> success(String message) {
        Result<T> r = new Result<>();
        r.setCode(200); r.setMessage(message);
        return r;
    }

    public static <T> Result<T> badRequest(String msg) { return fail(400, msg); }
    public static <T> Result<T> unauthorized(String msg) { return fail(401, msg != null ? msg : "未授权"); }
    public static <T> Result<T> forbidden(String msg) { return fail(403, msg != null ? msg : "无权限"); }
    public static <T> Result<T> notFound(String msg) { return fail(404, msg != null ? msg : "资源不存在"); }
    public static <T> Result<T> serverError(String msg) { return fail(500, msg != null ? msg : "服务器内部错误"); }
}
