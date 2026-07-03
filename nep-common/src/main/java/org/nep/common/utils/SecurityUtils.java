package org.nep.common.utils;

import org.nep.common.constant.Roles;
import org.nep.common.context.UserContext;
import org.nep.common.exception.BusinessException;

/**
 * 安全工具类：统一读取当前登录用户、角色判断与数据归属校验。
 * <p>Service 层做业务前置校验时使用，配合 {@link UserContext} 与
 * {@link org.nep.common.annotation.RequiresRole} 形成"接口级角色鉴权 + 行级归属鉴权"双层防护。</p>
 *
 * @author NEP Team
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户ID，未登录抛 401。
     */
    public static Long getUserId() {
        Long id = UserContext.getUserId();
        if (id == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return id;
    }

    /** 当前登录用户角色编码（可能为 null） */
    public static String getRole() {
        return UserContext.getRole();
    }

    /** 是否管理员 */
    public static boolean isAdmin() {
        return Roles.ADMIN.equals(UserContext.getRole());
    }

    /** 是否具备指定角色 */
    public static boolean hasRole(String role) {
        return role != null && role.equals(UserContext.getRole());
    }

    /**
     * 数据归属校验：仅资源所有者本人或管理员可操作，否则抛 403。
     * <p>用于"监督员只能查/撤回自己的反馈""网格员只能操作指派给自己的任务"等场景。</p>
     *
     * @param ownerId 资源所属用户ID（如反馈的 supervisorId、任务的 assignedInspectorId）
     */
    public static void checkOwner(Long ownerId) {
        if (ownerId == null) {
            throw new BusinessException(400, "资源归属信息缺失，无法校验权限");
        }
        if (isAdmin()) {
            return;
        }
        if (!ownerId.equals(getUserId())) {
            throw new BusinessException(403, "无权操作他人数据");
        }
    }
}
