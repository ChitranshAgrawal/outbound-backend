package com.addverb.outbound_service.inventory;

import lombok.Builder;
import lombok.Data;

import java.util.List;


@Data
@Builder
public class InventoryBulkDeductRequest {

    private String orderNumber;
    private String skuCode;
    private Integer requestedQty;
    private Integer alreadyAllocatedQty;
    private List<InventoryDeductRequest> allocations;
}

