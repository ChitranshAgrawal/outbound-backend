package com.addverb.outbound_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;


@Data
@Builder
public class BatchAllocationDetail {
    private String batchNo;
    private LocalDate expiryDate;
    private Double mrp;
    private Integer allocatedQty;
    private Integer quantity;
}



