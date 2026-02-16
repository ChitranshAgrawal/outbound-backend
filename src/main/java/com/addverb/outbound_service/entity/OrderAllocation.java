package com.addverb.outbound_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;


@Entity
@Table(name = "order_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_code", nullable = false)
    private String skuCode;

    @Column(name = "batch_no", nullable = false)
    private String batchNo;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    private Double mrp;

    @Column(name = "allocated_qty", nullable = false)
    private Integer allocatedQty;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

}



