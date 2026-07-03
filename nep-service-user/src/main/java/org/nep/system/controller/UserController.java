package org.nep.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nep.common.annotation.RequiresRole;
import org.nep.common.constant.Roles;
import org.nep.common.result.Result;
import org.nep.common.utils.SecurityUtils;
import org.nep.system.dto.LoginResult;
import org.nep.system.entity.User;
import org.nep.system.service.UserService;
import org.nep.system.service.impl.UserServiceImpl;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器。
 * <p>注册/登录对外开放；资料修改、改密以登录态为准并做归属校验；
 * 角色/状态变更、用户列表、删除为管理员(NEPM)专属，杜绝越权提权。</p>
 *
 * @author NEP Team
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final UserServiceImpl userServiceImpl;

    public UserController(UserService userService, UserServiceImpl userServiceImpl) {
        this.userService = userService;
        this.userServiceImpl = userServiceImpl;
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<User> register(@RequestBody User user) {
        return Result.ok("注册成功", userService.register(user));
    }

    @Operation(summary = "用户登录（返回JWT Token）")
    @PostMapping("/login")
    public Result<LoginResult> login(@RequestParam String phone, @RequestParam String password) {
        return Result.ok("登录成功", userServiceImpl.loginWithToken(phone, password));
    }

    @Operation(summary = "根据ID获取用户")
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.ok(userService.getById(id));
    }

    @Operation(summary = "用户列表（管理员）")
    @RequiresRole(Roles.ADMIN)
    @GetMapping("/list")
    public Result<java.util.List<User>> list() {
        return Result.ok(userService.list());
    }

    @Operation(summary = "网格员姓名列表（管理员派送任务用，问题①）")
    @RequiresRole(Roles.ADMIN)
    @GetMapping("/inspectors")
    public Result<java.util.List<java.util.Map<String, Object>>> inspectors() {
        // 仅查可用(status=1)的网格员，返回 id/姓名/工号 供按姓名派送
        java.util.List<User> list = userService.lambdaQuery()
                .eq(User::getRole, Roles.INSPECTOR)
                .eq(User::getStatus, 1)
                .list();
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (User u : list) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("realName", u.getRealName());
            m.put("employeeCode", u.getEmployeeCode());
            result.add(m);
        }
        return Result.ok(result);
    }

    @Operation(summary = "管理员重置用户密码（问题③）")
    @RequiresRole(Roles.ADMIN)
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        // 默认重置为 123456，也可由前端传入 newPassword 指定
        String newPassword = body.getOrDefault("newPassword", "123456");
        userService.resetPassword(id, newPassword);
        return Result.success("密码已重置");
    }

    @Operation(summary = "用户统计（管理员：总数/各角色数量）")
    @RequiresRole(Roles.ADMIN)
    @GetMapping("/stats")
    public Result<java.util.Map<String, Object>> stats() {
        java.util.List<User> all = userService.list();
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("total", all.size());
        result.put("supervisor", all.stream().filter(u -> "NEPS".equals(u.getRole())).count());
        result.put("inspector", all.stream().filter(u -> "NEPG".equals(u.getRole())).count());
        result.put("admin", all.stream().filter(u -> "NEPM".equals(u.getRole())).count());
        result.put("viewer", all.stream().filter(u -> "NEPV".equals(u.getRole())).count());
        return Result.ok(result);
    }

    @Operation(summary = "更新用户资料（本人或管理员；角色/状态仅管理员可改）")
    @PutMapping("/{id}")
    public Result<User> update(@PathVariable Long id, @RequestBody java.util.Map<String, Object> map) {
        // 归属校验：仅本人或管理员可修改该用户资料
        SecurityUtils.checkOwner(id);

        User user = new User();
        user.setId(id);
        if (map.containsKey("realName")) user.setRealName((String) map.get("realName"));
        if (map.containsKey("email")) user.setEmail((String) map.get("email"));
        if (map.containsKey("age")) user.setAge(map.get("age") != null ? ((Number) map.get("age")).intValue() : null);
        if (map.containsKey("gender")) user.setGender(map.get("gender") != null ? ((Number) map.get("gender")).intValue() : null);
        if (map.containsKey("avatar")) user.setAvatar((String) map.get("avatar"));

        // 角色与状态属敏感字段，仅管理员可变更，防止普通用户越权提权
        if (SecurityUtils.isAdmin()) {
            if (map.containsKey("status")) user.setStatus(map.get("status") != null ? ((Number) map.get("status")).intValue() : null);
            if (map.containsKey("role")) user.setRole((String) map.get("role"));
        }
        userService.updateById(user);
        return Result.success("更新成功");
    }

    @Operation(summary = "修改密码（本人）")
    @PutMapping("/change-password")
    public Result<Void> changePassword(@RequestBody java.util.Map<String, String> map) {
        String oldPassword = map.get("oldPassword");
        String newPassword = map.get("newPassword");
        if (oldPassword == null || newPassword == null) {
            return Result.fail("原密码和新密码不能为空");
        }
        if (newPassword.length() < 6) {
            return Result.fail("新密码至少6位");
        }
        // 仅能修改登录态本人的密码，忽略前端传入的 userId，防越权改他人密码
        userService.changePassword(SecurityUtils.getUserId(), oldPassword, newPassword);
        return Result.success("密码修改成功");
    }

    @Operation(summary = "删除用户（管理员）")
    @RequiresRole(Roles.ADMIN)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success("删除成功");
    }
}
