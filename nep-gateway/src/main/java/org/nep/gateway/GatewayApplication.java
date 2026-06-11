package org.nep.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * NEP API网关 - 统一入口，路由转发到各微服务
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("""
                ╔══════════════════════════════════════════════╗
                ║   🚪 NEP Gateway 网关启动成功                ║
                ║   端口: 9000                                ║
                ║   统一入口，路由转发到后端服务               ║
                ╚══════════════════════════════════════════════╝""");
    }
}
