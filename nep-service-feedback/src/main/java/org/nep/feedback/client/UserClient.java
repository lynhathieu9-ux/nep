package org.nep.feedback.client;

import org.nep.common.result.Result;
import org.nep.system.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

/**
 * OpenFeign 远程调用客户端 - 调用用户微服务
 * 用于获取公众监督员和网格员信息
 *
 * @author NEP Team
 */
@FeignClient(
    name = "nep-service-user",
    path = "/api/user",
    fallbackFactory = UserClientFallbackFactory.class
)
public interface UserClient {

    /**
     * 根据ID获取用户信息
     */
    @GetMapping("/{id}")
    Result<User> getById(@PathVariable("id") Long id);

    /**
     * 获取网格员姓名列表（问题①：按姓名派送任务时用于姓名→ID映射）
     */
    @GetMapping("/inspectors")
    Result<List<Map<String, Object>>> inspectors();
}
