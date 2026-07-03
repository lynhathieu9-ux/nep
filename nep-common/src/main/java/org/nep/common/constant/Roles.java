package org.nep.common.constant;

/**
 * 系统角色常量（与 nep_user.role 字段取值、JWT role 声明保持一致）。
 * <p>四端角色：监督员(NEPS)、网格员(NEPG)、管理员(NEPM)、决策者(NEPV)。
 * 统一在此定义，杜绝散落各处的魔法字符串。</p>
 *
 * @author NEP Team
 */
public final class Roles {

    private Roles() {
    }

    /** 公众监督员端 NEPS：上报空气质量监督信息 */
    public static final String SUPERVISOR = "NEPS";

    /** AQI 检测网格员端 NEPG：现场实测并提交 AQI 数据 */
    public static final String INSPECTOR = "NEPG";

    /** 系统管理端 NEPM：反馈指派、AQI 确认、统计管理 */
    public static final String ADMIN = "NEPM";

    /** 决策者端 NEPV：浏览统计、可视化大屏（只读） */
    public static final String DECISION = "NEPV";

    /**
     * 获取角色中文名称，用于日志/提示。
     *
     * @param role 角色编码
     * @return 中文名称，未知角色返回原编码
     */
    public static String displayName(String role) {
        if (role == null) {
            return "未知";
        }
        return switch (role) {
            case SUPERVISOR -> "监督员";
            case INSPECTOR -> "网格员";
            case ADMIN -> "管理员";
            case DECISION -> "决策者";
            default -> role;
        };
    }
}
