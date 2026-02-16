package com.addverb.outbound_service.dto;

import com.addverb.outbound_service.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
public class OrderDetailsResponse {

    private Long id;
    private String orderNumber;

    private String skuCode;

    private String customerName;
    private String address;

    private Integer requestedQty;
    private Integer allocatedQty;

    private OrderStatus status;

    private LocalDateTime createdAt;

    private List<OrderAllocationResponse> allocations;

}


