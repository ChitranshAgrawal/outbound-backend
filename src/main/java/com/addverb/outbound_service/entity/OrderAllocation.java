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

    @Column(name = "sku_code")
    private String skuCode;

    @Column(name = "batch_no")
    private String batchNo;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column()
    private Double mrp;

    @Column(name = "allocated_qty")
    private Integer allocatedQty;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

}



