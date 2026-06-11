package org.nep.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nep.common.result.Result;
import org.nep.system.dto.LoginResult;
import org.nep.system.entity.User;
import org.nep.system.service.UserService;
import org.nep.system.service.impl.UserServiceImpl;
import org.springframework.web.bind.annotation.*;

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
    public Result<User> getById(@PathVariable Long id) { return Result.ok(userService.getById(id)); }

    @Operation(summary = "用户列表")
    @GetMapping("/list")
    public Result<java.util.List<User>> list() { return Result.ok(userService.list()); }

    @Operation(summary = "更新用户（含头像）")
    @PutMapping("/{id}")
    public Result<User> update(@PathVariable Long id, @RequestBody java.util.Map<String, Object> map) {
        User user = new User();
        user.setId(id);
        if (map.containsKey("realName")) user.setRealName((String) map.get("realName"));
        if (map.containsKey("age")) user.setAge(map.get("age") != null ? ((Number) map.get("age")).intValue() : null);
        if (map.containsKey("gender")) user.setGender(map.get("gender") != null ? ((Number) map.get("gender")).intValue() : null);
        if (map.containsKey("avatar")) user.setAvatar((String) map.get("avatar"));
        if (map.containsKey("status")) user.setStatus(map.get("status") != null ? ((Number) map.get("status")).intValue() : null);
        if (map.containsKey("role")) user.setRole((String) map.get("role"));
        userService.updateById(user);
        return Result.success("更新成功");
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.removeById(id); return Result.success("删除成功");
    }
}
