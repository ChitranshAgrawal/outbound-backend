package com.addverb.outbound_service.service;

import com.addverb.outbound_service.dto.*;
import com.addverb.outbound_service.enums.OrderStatus;

import java.time.LocalDateTime;


public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request);

    PagedResponse<OrderResponse> getOrders(
            int page,
            int size,
            OrderStatus status,
            String skuCode,
            LocalDateTime fromDate,
            LocalDateTime toDate
    );

    AllocationResponse allocateOrder(String orderNumber);

    DashboardSummaryResponse getDashboardSummary();

    OrderDetailsResponse getOrderDetails(Long orderId);

    DashboardAnalyticsResponse getDashboardAnalytics();

}


