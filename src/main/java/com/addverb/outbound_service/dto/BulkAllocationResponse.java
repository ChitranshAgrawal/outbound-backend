package com.addverb.outbound_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BulkAllocationResponse {
    private int totalOrders;
    private int successCount;
    private int failureCount;
    private List<BulkOrderAllocationResult> results;
}




