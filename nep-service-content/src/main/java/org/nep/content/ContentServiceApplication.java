package org.nep.content;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 内容管理微服务 - 启动类
 * 负责新闻资讯、知识库、通知消息、AI对话、社交互动、统计分析
 *
 * @author NEP Team
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("org.nep.system.mapper")
@ComponentScan(basePackages = {"org.nep.content", "org.nep.system", "org.nep.framework", "org.nep.common"})
public class ContentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("  NEP Content Service 启动成功!");
        System.out.println("  端口: 8084");
        System.out.println("  注册中心: Nacos (localhost:8848)");
        System.out.println("========================================");
    }
}
