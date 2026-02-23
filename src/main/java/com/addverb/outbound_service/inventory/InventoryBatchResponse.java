package com.addverb.outbound_service.inventory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;


@Data
//@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryBatchResponse {
    private String batchNo;
    private LocalDate expiryDate;
    private Double mrp;
    private Integer quantity;
    private String sku;
    private String status;
}

