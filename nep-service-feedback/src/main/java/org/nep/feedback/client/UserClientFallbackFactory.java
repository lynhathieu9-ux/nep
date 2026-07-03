package org.nep.feedback.client;

import lombok.extern.slf4j.Slf4j;
import org.nep.common.result.Result;
import org.nep.system.entity.User;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Sentinel 熔断降级工厂 - 用户服务不可用时的兜底逻辑
 *
 * @author NEP Team
 */
@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        log.error("用户服务调用失败，触发熔断降级: {}", cause.getMessage());

        return new UserClient() {
            @Override
            public Result<User> getById(Long id) {
                // 返回一个默认用户，保证核心流程不中断
                log.warn("降级处理：返回默认用户信息, userId={}", id);
                User fallbackUser = new User();
                fallbackUser.setId(id);
                fallbackUser.setRealName("用户" + id);
                fallbackUser.setPhone("00000000000");
                return Result.ok("降级数据（用户服务暂不可用）", fallbackUser);
            }

            @Override
            public Result<List<Map<String, Object>>> inspectors() {
                // 用户服务不可用时返回空列表，避免派送流程 NPE
                log.warn("降级处理：网格员列表返回空");
                return Result.ok("降级数据（用户服务暂不可用）", Collections.emptyList());
            }
        };
    }
}
