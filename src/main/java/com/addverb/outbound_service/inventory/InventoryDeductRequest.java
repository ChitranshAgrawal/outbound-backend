package com.addverb.outbound_service.inventory;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class InventoryDeductRequest {
//    private String skuCode;
    private String batchNo;
    private Integer quantity;
//    private Double mrp;
}

