package main.java.main.java.com.example.productservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        Server devServer = new Server();
        devServer.setUrl("http://localhost:8083");
        devServer.setDescription("Development server");

        Server prodServer = new Server();
        prodServer.setUrl("https://api.productservice.com");
        prodServer.setDescription("Production server");

        Info info = new Info()
                .title("Product Service API")
                .version("1.0.0")
                .description("Comprehensive Product Management API with CRUD, Search, Filter, and Pagination capabilities")
                .contact(new Contact()
                        .name("Product Service Team")
                        .email("support@productservice.com")
                        .url("https://productservice.com"))
                .termsOfService("https://productservice.com/terms")
                .license(new io.swagger.v3.oas.models.info.License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0"));

        return new OpenAPI()
                .info(info)
                .servers(Arrays.asList(devServer, prodServer));
    }
}