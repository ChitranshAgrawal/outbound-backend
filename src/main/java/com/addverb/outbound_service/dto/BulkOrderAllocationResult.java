package com.addverb.outbound_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BulkOrderAllocationResult {
    private String orderNumber;
    private boolean success;
    private String message;
    private AllocationResponse allocation;
}

