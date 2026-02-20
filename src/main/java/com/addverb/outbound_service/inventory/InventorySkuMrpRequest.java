package com.addverb.outbound_service.inventory;

import lombok.Builder;

@Builder
public record InventorySkuMrpRequest(
        String sku,
        Double mrp
) {
}