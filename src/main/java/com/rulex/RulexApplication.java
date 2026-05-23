package com.rulex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.rulex.config.RuleEngineProperties;

@SpringBootApplication
@EnableConfigurationProperties(RuleEngineProperties.class)
public class RulexApplication {

    public static void main(String[] args) {
        SpringApplication.run(RulexApplication.class, args);
    }
}
