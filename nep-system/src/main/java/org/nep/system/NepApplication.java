package org.nep.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * NEP 东软环保公众监督系统 - 分布式启动类
 */
@SpringBootApplication(scanBasePackages = "org.nep")
@MapperScan("org.nep.system.mapper")
@EnableDiscoveryClient
public class NepApplication {
    public static void main(String[] args) {
        SpringApplication.run(NepApplication.class, args);
        System.out.println("""
                🌿 东软环保公众监督系统 NEP v1.0
                   分布式架构: 多模块Maven + MyBatis-Plus
                """);
    }
}
