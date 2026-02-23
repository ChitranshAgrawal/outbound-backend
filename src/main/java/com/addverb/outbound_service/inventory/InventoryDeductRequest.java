package com.addverb.outbound_service.inventory;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;


@Data
@Builder
public class InventoryDeductRequest {
//    private String skuCode;
    private String batchNo;
    private String sku;
    private Integer quantity;
    private Double mrp;
    private String status;
    private LocalDate expiryDate;
}
