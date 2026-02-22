package com.addverb.outbound_service.inventory;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InventoryBulkOrdersDeductRequest {
    private String operation;
    private List<InventoryDeductRequest> items;
}


