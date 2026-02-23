package com.addverb.outbound_service.service;

import com.addverb.outbound_service.dto.*;
import com.addverb.outbound_service.enums.OrderExportDateFilter;
import com.addverb.outbound_service.enums.OrderStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


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

    BulkAllocationResponse allocateOrdersBulk(List<String> orderNumbers);

    DashboardSummaryResponse getDashboardSummary();

    OrderDetailsResponse getOrderDetails(Long orderId);

    DashboardAnalyticsResponse getDashboardAnalytics();

    byte[] exportOrders(OrderExportDateFilter filter, LocalDate startDate, LocalDate endDate);

}



