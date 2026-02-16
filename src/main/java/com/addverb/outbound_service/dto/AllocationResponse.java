package com.addverb.outbound_service.dto;

import com.addverb.outbound_service.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;


@Data
@Builder
public class AllocationResponse {
    private String orderNumber;
    private Integer requestedQty;
    private Integer allocatedQty;
    private OrderStatus status;
    private List<BatchAllocationDetail> allocations;
}

