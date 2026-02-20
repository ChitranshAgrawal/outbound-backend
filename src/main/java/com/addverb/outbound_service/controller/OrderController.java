package com.addverb.outbound_service.controller;

import com.addverb.outbound_service.common.ApiResponse;
import com.addverb.outbound_service.dto.*;
import com.addverb.outbound_service.enums.OrderStatus;
import com.addverb.outbound_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;


@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);

        return ResponseEntity.ok(
                ApiResponse.<OrderResponse>builder()
                        .success(true)
                        .message("Order created successfully")
                        .data(response)
                        .build()
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)OrderStatus status,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate
            ) {

//        LocalDateTime from = fromDate != null ? LocalDateTime.parse(fromDate) : null;
//        LocalDateTime to = toDate != null ? LocalDateTime.parse(toDate) : null;
//
//        PagedResponse<OrderResponse> response = orderService.getOrders(page, size, status, skuCode, from, to);

        PagedResponse<OrderResponse> response = orderService.getOrders(page, size, status, skuCode, fromDate, toDate);

        return ResponseEntity.ok(
                ApiResponse.<PagedResponse<OrderResponse>>builder()
                        .success(true)
                        .message("Orders fetched successfully")
                        .data(response)
                        .build()
        );
    }

    @PostMapping("/{orderNumber}/allocate")
    public ResponseEntity<ApiResponse<AllocationResponse>> allocateOrder(@PathVariable String orderNumber) {

        AllocationResponse response = orderService.allocateOrder(orderNumber);

        return ResponseEntity.ok(
                ApiResponse.<AllocationResponse>builder()
                        .success(true)
                        .message("Allocation Completed Successfully")
                        .data(response)
                        .build()
        );
    }

    @PostMapping("/allocate/bulk")
    public ResponseEntity<ApiResponse<BulkAllocationResponse>> allocateOrdersBulk(
            @Valid @RequestBody BulkAllocateOrdersRequest request) {

        BulkAllocationResponse response =
                orderService.allocateOrdersBulk(request.getOrderNumbers());

        return ResponseEntity.ok(
                ApiResponse.<BulkAllocationResponse>builder()
                        .success(true)
                        .message("Bulk allocation processed")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {

        DashboardSummaryResponse summary =
                orderService.getDashboardSummary();

        return ResponseEntity.ok(
                ApiResponse.<DashboardSummaryResponse>builder()
                        .success(true)
                        .message("Dashboard summary fetched successfully")
                        .data(summary)
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDetailsResponse>> getOrderDetails(
            @PathVariable Long id) {

        OrderDetailsResponse response =
                orderService.getOrderDetails(id);

        return ResponseEntity.ok(
                ApiResponse.<OrderDetailsResponse>builder()
                        .success(true)
                        .message("Order details fetched successfully")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<DashboardAnalyticsResponse>> getAnalytics() {

        DashboardAnalyticsResponse response =
                orderService.getDashboardAnalytics();

        return ResponseEntity.ok(
                ApiResponse.<DashboardAnalyticsResponse>builder()
                        .success(true)
                        .message("Dashboard analytics fetched successfully")
                        .data(response)
                        .build()
        );
    }

}







