package org.nep.framework.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("东软环保公众监督系统 API")
                        .version("1.0.0")
                        .description("NEP 分布式环保公众监督系统接口文档")
                        .contact(new Contact().name("第1组").email("nep@neusoft.com")));
    }
}
