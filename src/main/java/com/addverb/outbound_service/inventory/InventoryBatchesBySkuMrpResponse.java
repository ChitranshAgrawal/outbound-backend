package com.addverb.outbound_service.inventory;

import lombok.Data;

import java.util.List;

@Data
public class InventoryBatchesBySkuMrpResponse {
    private String sku;
    private Double mrp;
    private List<InventoryBatchResponse> batches;
}


