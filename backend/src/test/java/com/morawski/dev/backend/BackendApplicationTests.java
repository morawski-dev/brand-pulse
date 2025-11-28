package com.morawski.dev.backend;

import com.morawski.dev.backend.config.OpenRouterConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class, OpenRouterConfig.class})
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
