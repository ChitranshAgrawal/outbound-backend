package com.addverb.outbound_service.dto;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class DashboardSummaryResponse {
    private long totalOrders;
    private long pendingOrders;
    private long partialOrders;
    private long completedOrders;
    private long todayOrders;
    private long totalAllocatedQuantity;
}



