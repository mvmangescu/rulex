package com.rulex.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI(@Value("${server.port:8080}") int port) {
        return new OpenAPI()
                .info(new Info()
                        .title("Rulex API")
                        .description("Production-ready rule engine — evaluate boolean expressions against a variable context, " +
                                "store and manage named rules, and inspect evaluation traces.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Rulex")
                                .url("https://github.com/rulex")))
                .servers(List.of(
                        new Server().url("http://localhost:" + port).description("Local")));
    }
}
