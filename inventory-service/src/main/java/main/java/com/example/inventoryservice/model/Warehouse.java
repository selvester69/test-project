package main.java.main.java.com.example.inventoryservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

@Entity
@Table(name = "warehouses")
@org.hibernate.annotations.Table(appliesTo = "warehouses", indexes = {
    @org.hibernate.annotations.Index(name = "idx_warehouse_name", columnNames = "name"),
    @org.hibernate.annotations.Index(name = "idx_warehouse_location", columnNames = "location"),
    @org.hibernate.annotations.Index(name = "idx_warehouse_status", columnNames = "status"),
    @org.hibernate.annotations.Index(name = "idx_warehouse_created_at", columnNames = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Warehouse name is required")
    @Size(min = 1, max = 255, message = "Warehouse name must be between 1 and 255 characters")
    @Column(nullable = false)
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotBlank(message = "Location is required")
    @Size(min = 2, max = 100, message = "Location must be between 2 and 100 characters")
    @Column(nullable = false)
    private String location;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    @Column(columnDefinition = "TEXT")
    private String address;

    @Min(value = 0, message = "Capacity cannot be negative")
    @Column(nullable = false)
    private Integer capacity = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WarehouseStatus status = WarehouseStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum WarehouseStatus {
        ACTIVE, INACTIVE
    }
}