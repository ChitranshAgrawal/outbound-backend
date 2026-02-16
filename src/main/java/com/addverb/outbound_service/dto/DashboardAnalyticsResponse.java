package com.addverb.outbound_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;


@Data
@Builder
public class DashboardAnalyticsResponse {

    private Map<String, Long> statusDistribution;

    private List<DailyOrderData> dailyOrders;

    private List<DailyAllocationData> dailyAllocations;
}

