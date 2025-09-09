package main.java.main.java.com.example.inventoryservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "inventory_items")
@org.hibernate.annotations.Table(appliesTo = "inventory_items", indexes = {
    @org.hibernate.annotations.Index(name = "idx_inventory_product_id", columnNames = "product_id"),
    @org.hibernate.annotations.Index(name = "idx_inventory_warehouse_id", columnNames = "warehouse_id"),
    @org.hibernate.annotations.Index(name = "idx_inventory_status", columnNames = "status"),
    @org.hibernate.annotations.Index(name = "idx_inventory_available_quantity", columnNames = "available_quantity"),
    @org.hibernate.annotations.Index(name = "idx_inventory_created_at", columnNames = "created_at"),
    @org.hibernate.annotations.Index(name = "idx_inventory_composite", columnNames = {"product_id", "warehouse_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Min(value = 0, message = "Total quantity cannot be negative")
    @Column(nullable = false)
    private Integer totalQuantity = 0;

    @Min(value = 0, message = "Reserved quantity cannot be negative")
    @Column(nullable = false)
    private Integer reservedQuantity = 0;

    @Transient
    public Integer getAvailableQuantity() {
        return Math.max(0, totalQuantity - reservedQuantity);
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryStatus status = InventoryStatus.ACTIVE;

    @Type(type = "json")
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new java.util.HashMap<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        updateStatus();
    }

    @PostLoad
    protected void onLoad() {
        updateStatus();
    }

    public void updateStatus() {
        int available = getAvailableQuantity();
        if (available == 0) {
            status = InventoryStatus.OUT_OF_STOCK;
        } else if (available < 10) {
            status = InventoryStatus.LOW_STOCK;
        } else {
            status = InventoryStatus.ACTIVE;
        }
    }

    public enum InventoryStatus {
        ACTIVE, LOW_STOCK, OUT_OF_STOCK
    }
}