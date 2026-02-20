package com.addverb.outbound_service.dto;

import com.addverb.outbound_service.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@Builder
public class OrderResponse {

    private String orderNumber;
    private String customerName;
    private String address;
    private String skuCode;
    private Double mrp;
    private Integer requestedQty;
    private Integer allocatedQty;
    private OrderStatus status;
    private LocalDateTime createdAt;
}


