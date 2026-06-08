package com.mock.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.net.InetAddress;

@SpringBootApplication
public class MockApplication {

    private static final Logger log = LoggerFactory.getLogger(MockApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(MockApplication.class, args);
    }

    @Bean
    ApplicationRunner apiInfo(Environment env) {
        return args -> {
            String port = env.getProperty("server.port", "8080");
            String ip = InetAddress.getLocalHost().getHostAddress();
            log.info("========================================");
            log.info("  知识服务已启动");
            log.info("  API: POST http://{}:{}/api/knowledge", ip, port);
            log.info("  RM1201 → entName + moduleCode");
            log.info("  RM1202 → key");
            log.info("========================================");
        };
    }
}
