package com.addverb.outbound_service.inventory;

import lombok.Data;

import java.time.LocalDate;


@Data
public class InventoryBatchResponse {
    private String batchNo;
    private LocalDate expiryDate;
    private Double mrp;
    private Integer quantity;
//    private String sku;
}




