package com.addverb.outbound_service.inventory;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Builder
@ToString
@Data
public class InventorySkuMrpRequest {
        String sku;
        Double mrp;

    public InventorySkuMrpRequest(String sku, Double mrp) {
        this.sku = sku;
        this.mrp = mrp;
    }

}