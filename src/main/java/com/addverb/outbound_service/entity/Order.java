package com.addverb.outbound_service.entity;

import com.addverb.outbound_service.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String address;

    @Column(name = "sku_code", nullable = false)
    private String skuCode;

    @Column(name = "mrp", nullable = false)
    private Double mrp;

    @Column(name = "requested_qty", nullable = false)
    private Integer requestedQty;

    @Column(name = "allocated_qty", nullable = false)
    private Integer allocatedQty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderAllocation> allocations;

}




