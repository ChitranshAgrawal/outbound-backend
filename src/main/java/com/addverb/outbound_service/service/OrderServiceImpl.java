package com.addverb.outbound_service.service;

import com.addverb.outbound_service.dto.*;
import com.addverb.outbound_service.entity.Order;
import com.addverb.outbound_service.entity.OrderAllocation;
import com.addverb.outbound_service.enums.OrderStatus;
import com.addverb.outbound_service.exception.AllocationException;
import com.addverb.outbound_service.exception.BusinessException;
import com.addverb.outbound_service.exception.OrderNotFoundException;
import com.addverb.outbound_service.inventory.InventoryBatchResponse;
import com.addverb.outbound_service.inventory.InventoryClient;
import com.addverb.outbound_service.inventory.InventoryDeductRequest;
import com.addverb.outbound_service.repository.OrderAllocationRepository;
import com.addverb.outbound_service.repository.OrderRepository;
import com.addverb.outbound_service.specification.OrderSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final OrderAllocationRepository allocationRepository;

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customerName(request.getCustomerName())
                .address(request.getAddress())
                .skuCode(request.getSkuCode())
                .requestedQty(request.getRequestedQty())
                .allocatedQty(0)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Order saved = orderRepository.save(order);

        return mapToResponse(saved);
    }

    @Override
    public PagedResponse<OrderResponse> getOrders(
            int page,
            int size,
            OrderStatus status,
            String skuCode,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<Order> spec = Specification
                .where(OrderSpecification.hasStatus(status))
                .and(OrderSpecification.hasSkuCode(skuCode))
                .and(OrderSpecification.createdAfter(fromDate))
                .and(OrderSpecification.createdBefore(toDate));

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);

        List<OrderResponse> content = orderPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return PagedResponse.<OrderResponse>builder()
                .content(content)
                .currentPage(orderPage.getNumber())
                .totalPages(orderPage.getTotalPages())
                .totalElements(orderPage.getTotalElements())
                .build();
    }

    private String generateOrderNumber() {
//        return "ORD-" + UUID.randomUUID().toString().substring(0, 8);
        String orderNumber;
        do {
            orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (orderRepository.findByOrderNumber(orderNumber).isPresent());

        return orderNumber;
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .orderNumber(order.getOrderNumber())
                .customerName(order.getCustomerName())
                .address(order.getAddress())
                .skuCode(order.getSkuCode())
                .requestedQty(order.getRequestedQty())
                .allocatedQty(order.getAllocatedQty())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public AllocationResponse allocateOrder(String orderNumber) {

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new BusinessException("Order not found"));

        if (order.getStatus() == OrderStatus.COMPLETED)
            throw new AllocationException("Order already completed");

        int remainingQty = order.getRequestedQty() - order.getAllocatedQty();

        if (remainingQty <= 0)
            throw new AllocationException("Order already fully allocated");

        List<InventoryBatchResponse> batches = inventoryClient.getBatchesBySku(order.getSkuCode());

        List<InventoryBatchResponse> validBatches = batches.stream()
                .filter(Objects::nonNull)
                .filter(batch -> batch.getBatchNo() != null && !batch.getBatchNo().isBlank())
                .filter(batch -> batch.getExpiryDate() != null && batch.getExpiryDate().isAfter(java.time.LocalDate.now()))
                .filter(batch -> batch.getAvailableQty() != null && batch.getAvailableQty() > 0)
                .sorted(Comparator.comparing(InventoryBatchResponse::getExpiryDate))
                .toList();

        if (validBatches.isEmpty())
            throw new AllocationException("No valid inventory available");

        int totalAllocatedNow = 0;

        List<BatchAllocationDetail> allocationDetails = new java.util.ArrayList<>();

        for (InventoryBatchResponse batch: validBatches) {

            if (remainingQty <= 0)
                break;

            int available = batch.getAvailableQty();

            if (available <= 0)
                continue;

            int allocateQty = Math.min(remainingQty, available);

            allocationDetails.add(
                    BatchAllocationDetail.builder()
                            .batchNo(batch.getBatchNo())
                            .expiryDate(batch.getExpiryDate())
                            .mrp(batch.getMrp())
                            .allocatedQty(allocateQty)
                            .build()
            );

            remainingQty -= allocateQty;
            totalAllocatedNow += allocateQty;
        }

        if (totalAllocatedNow == 0)
            throw new AllocationException("Insufficient stock for allocation");

//        for (BatchAllocationDetail detail : allocationDetails) {
//
//            inventoryClient.deductInventory(
//                    order.getSkuCode(),
//                    detail.getBatchNo(),
//                    detail.getAllocatedQty()
//            );
//        }

        List<InventoryDeductRequest> deductRequests = allocationDetails.stream()
                .map(detail -> InventoryDeductRequest.builder()
                        .skuCode(order.getSkuCode())
                        .batchNo(detail.getBatchNo())
                        .quantity(detail.getAllocatedQty())
                        .build()
                )
                .toList();

        inventoryClient.deductInventoryBulk(
                order.getOrderNumber(),
                order.getSkuCode(),
                order.getRequestedQty(),
                order.getAllocatedQty(),
                deductRequests
        );

        for (BatchAllocationDetail detail : allocationDetails) {

            OrderAllocation allocation = OrderAllocation.builder()
                    .order(order)
                    .skuCode(order.getSkuCode())
                    .batchNo(detail.getBatchNo())
                    .expiryDate(detail.getExpiryDate())
                    .mrp(detail.getMrp())
                    .allocatedQty(detail.getAllocatedQty())
                    .build();

            allocationRepository.save(allocation);
        }

        order.setAllocatedQty(order.getAllocatedQty() + totalAllocatedNow);

        if (order.getAllocatedQty().equals(order.getRequestedQty()))
            order.setStatus(OrderStatus.COMPLETED);
        else if (order.getAllocatedQty() >0)
            order.setStatus(OrderStatus.PARTIAL);

        order.setUpdatedAt(java.time.LocalDateTime.now());
        orderRepository.save(order);

        return AllocationResponse.builder()
                .orderNumber(order.getOrderNumber())
                .requestedQty(order.getRequestedQty())
                .allocatedQty(order.getAllocatedQty())
                .status(order.getStatus())
                .allocations(allocationDetails)
                .build();
    }

    @Override
    public DashboardSummaryResponse getDashboardSummary() {

        long totalOrders = orderRepository.count();

        long pending = orderRepository.countByStatus(OrderStatus.PENDING);
        long partial = orderRepository.countByStatus(OrderStatus.PARTIAL);
        long completed = orderRepository.countByStatus(OrderStatus.COMPLETED);

        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        long todayOrders = orderRepository.countByCreatedAtBetween(
                startOfDay,
                endOfDay
        );

        long totalAllocatedQty =
                orderRepository.getTotalAllocatedQuantity();

        return DashboardSummaryResponse.builder()
                .totalOrders(totalOrders)
                .pendingOrders(pending)
                .partialOrders(partial)
                .completedOrders(completed)
                .todayOrders(todayOrders)
                .totalAllocatedQuantity(totalAllocatedQty)
                .build();
    }

    @Override
    public OrderDetailsResponse getOrderDetails(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException(orderId)
                );

        List<OrderAllocationResponse> allocationResponses =
                order.getAllocations()
                        .stream()
                        .map(allocation -> OrderAllocationResponse.builder()
                                .batchNo(allocation.getBatchNo())
                                .expDate(allocation.getExpiryDate())
                                .mrp(allocation.getMrp())
                                .allocatedQty(allocation.getAllocatedQty())
                                .build())
                        .toList();

        return OrderDetailsResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .skuCode(order.getSkuCode())
                .customerName(order.getCustomerName())
                .address(order.getAddress())
                .requestedQty(order.getRequestedQty())
                .allocatedQty(order.getAllocatedQty())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .allocations(allocationResponses)
                .build();
    }

    @Override
    public DashboardAnalyticsResponse getDashboardAnalytics() {

        List<Object[]> statusData = orderRepository.getStatusDistribution();

        Map<String, Long> statusMap = statusData.stream()
                .collect(Collectors.toMap(
                        obj -> obj[0].toString(),
                        obj -> (Long) obj[1]
                ));

        LocalDateTime last7Days =
                LocalDateTime.now().minusDays(7);

        List<Object[]> dailyOrdersRaw =
                orderRepository.getDailyOrders(last7Days);

        List<DailyOrderData> dailyOrders =
                dailyOrdersRaw.stream()
                        .map(obj -> DailyOrderData.builder()
                                .date(((java.sql.Date) obj[0]).toLocalDate())
                                .orderCount((Long) obj[1])
                                .build())
                        .toList();

        List<Object[]> allocationRaw =
                orderRepository.getDailyAllocation(last7Days);

        List<DailyAllocationData> dailyAllocations =
                allocationRaw.stream()
                        .map(obj -> DailyAllocationData.builder()
                                .date(((java.sql.Date) obj[0]).toLocalDate())
                                .allocatedQty(obj[1] != null ? (Long) obj[1] : 0L)
                                .build())
                        .toList();

        return DashboardAnalyticsResponse.builder()
                .statusDistribution(statusMap)
                .dailyOrders(dailyOrders)
                .dailyAllocations(dailyAllocations)
                .build();
    }

}









