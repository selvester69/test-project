package main.java.main.java.com.example.inventoryservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
            .info(new Info()
                .title("Inventory Service API")
                .version("1.0.0")
                .description("Inventory Management Service API for stock tracking, warehouse operations, and event-driven integration")
                .contact(new Contact()
                    .name("Inventory Service Team")
                    .email("inventory@company.com")
                    .url("https://inventory.company.com")
                )
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")
                )
            )
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName, createAPIKeyScheme(securitySchemeName))
                .addSchemas("InventoryItem", createInventoryItemSchema())
                .addSchemas("Warehouse", createWarehouseSchema())
                .addSchemas("StockEvent", createStockEventSchema())
                .addSchemas("LowStockAlert", createLowStockAlertSchema())
                .addSchemas("PaginationResponse", createPaginationResponseSchema())
                .addSchemas("ErrorResponse", createErrorResponseSchema())
            );
    }

    private SecurityScheme createAPIKeyScheme(String name) {
        return new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .description("Provide the JWT token for authentication")
            .bearerFormat("JWT")
            .scheme("bearer")
            .name(name);
    }

    private Schema<?> createInventoryItemSchema() {
        Schema<?> schema = new ObjectSchema()
            .description("Inventory item representing stock levels for a product in a warehouse");
        
        schema.addProperty("id", new StringSchema().description("Unique identifier")._default("0"));
        schema.addProperty("productId", new StringSchema().description("Referenced product ID")._default("0"));
        schema.addProperty("warehouse", new StringSchema().description("Warehouse reference")._default(""));
        schema.addProperty("totalQuantity", new StringSchema().description("Total stock quantity")._default("0"));
        schema.addProperty("reservedQuantity", new StringSchema().description("Reserved stock quantity")._default("0"));
        schema.addProperty("availableQuantity", new StringSchema().description("Calculated available quantity")._default("0"));
        
        Schema<?> statusSchema = new StringSchema();
        statusSchema._enum(List.of("ACTIVE", "LOW_STOCK", "OUT_OF_STOCK"));
        schema.addProperty("status", statusSchema);
        
        schema.addProperty("metadata", new ObjectSchema().description("Extensible metadata"));
        schema.addProperty("createdAt", new StringSchema().format("date-time"));
        schema.addProperty("updatedAt", new StringSchema().format("date-time"));
        
        return schema;
    }

    private Schema<?> createWarehouseSchema() {
        Schema<?> schema = new ObjectSchema()
            .description("Warehouse location and capacity information");
        
        schema.addProperty("id", new StringSchema()._default("0"));
        schema.addProperty("name", new StringSchema().description("Warehouse name"));
        schema.addProperty("location", new StringSchema().description("Geographic location"));
        schema.addProperty("address", new StringSchema().description("Full address"));
        schema.addProperty("capacity", new StringSchema().description("Maximum capacity")._default("0"));
        
        Schema<?> statusSchema = new StringSchema();
        statusSchema._enum(List.of("ACTIVE", "INACTIVE"));
        schema.addProperty("status", statusSchema);
        
        return schema;
    }

    private Schema<?> createStockEventSchema() {
        Schema<?> schema = new ObjectSchema()
            .description("Event representing stock level changes");
        
        schema.addProperty("eventType", new StringSchema().description("Type of stock event"));
        schema.addProperty("productId", new StringSchema()._default("0"));
        schema.addProperty("warehouseId", new StringSchema()._default("0"));
        schema.addProperty("oldQuantity", new StringSchema()._default("0"));
        schema.addProperty("newQuantity", new StringSchema()._default("0"));
        schema.addProperty("availableQuantity", new StringSchema()._default("0"));
        schema.addProperty("status", new StringSchema());
        schema.addProperty("operation", new StringSchema().description("Type of operation: CREATE, UPDATE, DELETE"));
        schema.addProperty("timestamp", new StringSchema().format("int64"));
        schema.addProperty("service", new StringSchema().description("Service that generated the event"));
        
        return schema;
    }

    private Schema<?> createLowStockAlertSchema() {
        Schema<?> schema = new ObjectSchema()
            .description("Alert for low stock levels");
        
        schema.addProperty("productId", new StringSchema()._default("0"));
        schema.addProperty("warehouseId", new StringSchema()._default("0"));
        schema.addProperty("availableQuantity", new StringSchema()._default("0"));
        schema.addProperty("threshold", new StringSchema()._default("10"));
        
        Schema<?> severitySchema = new StringSchema();
        severitySchema._enum(List.of("WARNING", "CRITICAL"));
        schema.addProperty("severity", severitySchema);
        
        schema.addProperty("timestamp", new StringSchema().format("date-time"));
        
        return schema;
    }

    private Schema<?> createPaginationResponseSchema() {
        Schema<?> schema = new ObjectSchema()
            .description("Paginated response wrapper");
        
        ArraySchema contentSchema = new ArraySchema();
        contentSchema.items(new ObjectSchema().$ref("#/components/schemas/InventoryItem"));
        schema.addProperty("content", contentSchema);
        
        schema.addProperty("page", new StringSchema()._default("0"));
        schema.addProperty("size", new StringSchema()._default("20"));
        schema.addProperty("totalElements", new StringSchema()._default("0"));
        schema.addProperty("totalPages", new StringSchema()._default("1"));
        schema.addProperty("first", new StringSchema()._default("true"));
        schema.addProperty("last", new StringSchema()._default("true"));
        schema.addProperty("numberOfElements", new StringSchema()._default("0"));
        schema.addProperty("sort", new ObjectSchema().additionalProperties(true));
        
        return schema;
    }

    private Schema<?> createErrorResponseSchema() {
        Schema<?> schema = new ObjectSchema()
            .description("Standard error response format");
        
        schema.addProperty("timestamp", new StringSchema().format("date-time"));
        schema.addProperty("status", new StringSchema()._default("500"));
        schema.addProperty("error", new StringSchema());
        schema.addProperty("message", new StringSchema());
        schema.addProperty("path", new StringSchema());
        schema.addProperty("requestId", new StringSchema());
        
        ArraySchema validationErrorsSchema = new ArraySchema();
        validationErrorsSchema.items(new ObjectSchema());
        schema.addProperty("validationErrors", validationErrorsSchema);
        
        return schema;
    }
}