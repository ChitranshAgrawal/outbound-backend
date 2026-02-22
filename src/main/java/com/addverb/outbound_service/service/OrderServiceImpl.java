package com.addverb.outbound_service.service;

import com.addverb.outbound_service.dto.*;
import com.addverb.outbound_service.entity.Order;
import com.addverb.outbound_service.entity.OrderAllocation;
import com.addverb.outbound_service.enums.OrderStatus;
import com.addverb.outbound_service.exception.AllocationException;
import com.addverb.outbound_service.exception.BusinessException;
import com.addverb.outbound_service.exception.InventoryServiceException;
import com.addverb.outbound_service.exception.OrderNotFoundException;
import com.addverb.outbound_service.inventory.InventoryBatchResponse;
import com.addverb.outbound_service.inventory.InventoryClient;
import com.addverb.outbound_service.inventory.InventoryDeductRequest;
import com.addverb.outbound_service.inventory.InventoryOrderDeductPlan;
import com.addverb.outbound_service.repository.OrderAllocationRepository;
import com.addverb.outbound_service.repository.OrderRepository;
import com.addverb.outbound_service.specification.OrderSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.addverb.outbound_service.enums.OrderStatus.PENDING;


@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final OrderAllocationRepository allocationRepository;
    private final PlatformTransactionManager transactionManager;

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customerName(request.getCustomerName())
                .address(request.getAddress())
                .skuCode(request.getSkuCode())
                .mrp(request.getMrp())
                .requestedQty(request.getRequestedQty())
                .allocatedQty(0)
                .status(PENDING)
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

        if ( fromDate != null && toDate != null && fromDate.isAfter(toDate) )
            throw new BusinessException("From Date must be less than To Date");

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
                .mrp(order.getMrp())
                .requestedQty(order.getRequestedQty())
                .allocatedQty(order.getAllocatedQty())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public AllocationResponse allocateOrder(String orderNumber) {
        return allocateOrderInternal(orderNumber);
    }

    @Override
    @Transactional
    public BulkAllocationResponse allocateOrdersBulk(List<String> orderNumbers) {

        if (orderNumbers == null || orderNumbers.isEmpty()) {
            return BulkAllocationResponse.builder()
                    .totalOrders(0)
                    .successCount(0)
                    .failureCount(0)
                    .results(java.util.Collections.emptyList())
                    .build();
        }

        List<BulkOrderAllocationResult> results = new java.util.ArrayList<>();

        Set<String> normalizedOrderNumbers = orderNumbers.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(orderNumber -> !orderNumber.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (String orderNumber : orderNumbers) {
            if (orderNumber == null || orderNumber.trim().isBlank()) {
                results.add(BulkOrderAllocationResult.builder()
                        .orderNumber(orderNumber)
                        .success(false)
                        .message("Order number is missing")
                        .build());
            }
        }

        if (normalizedOrderNumbers.isEmpty()) {
            int failureCount = results.size();
            return BulkAllocationResponse.builder()
                    .totalOrders(results.size())
                    .successCount(0)
                    .failureCount(failureCount)
                    .results(results)
                    .build();
        }

        List<Order> existingOrders = orderRepository.findByOrderNumberInForUpdate(new java.util.ArrayList<>(normalizedOrderNumbers));

        Map<String, Order> orderMap = existingOrders.stream()
                .collect(Collectors.toMap(Order::getOrderNumber, order -> order));

        List<Order> allocatableOrders = new java.util.ArrayList<>();

        for (String orderNumber : normalizedOrderNumbers) {

            Order order = orderMap.get(orderNumber);

            if (order == null) {
                results.add(BulkOrderAllocationResult.builder()
                        .orderNumber(orderNumber)
                        .success(false)
                        .message("Allocation failed for order " + orderNumber + ": Order not found")
                        .build());
                continue;
            }

            if (order.getStatus() == OrderStatus.COMPLETED) {
                results.add(BulkOrderAllocationResult.builder()
                        .orderNumber(orderNumber)
                        .success(false)
                        .message("Allocation failed for order " + orderNumber + ": Order already completed")
                        .build());
                continue;
            }

            int remainingQty = order.getRequestedQty() - order.getAllocatedQty();

            if (remainingQty <= 0) {
                results.add(BulkOrderAllocationResult.builder()
                        .orderNumber(orderNumber)
                        .success(false)
                        .message("Allocation failed for order " + orderNumber + ": Order already fully allocated")
                        .build());
                continue;
            }

            allocatableOrders.add(order);
        }

        if (allocatableOrders.isEmpty()) {
            int successCount = (int) results.stream().filter(BulkOrderAllocationResult::isSuccess).count();
            int failureCount = results.size() - successCount;
            return BulkAllocationResponse.builder()
                    .totalOrders(results.size())
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .results(results)
                    .build();
        }

        List<InventoryClient.OrderInventoryQuery> queries = allocatableOrders.stream()
                .map(order -> new InventoryClient.OrderInventoryQuery(order.getSkuCode(), order.getMrp()))
                .toList();

        Map<String, List<InventoryBatchResponse>> inventoryBySkuMrp;
        try {
            inventoryBySkuMrp = inventoryClient.getBatchesBySkuAndMrpBulk(queries);
        } catch (InventoryServiceException ex) {
            for (Order order : allocatableOrders) {
                results.add(BulkOrderAllocationResult.builder()
                        .orderNumber(order.getOrderNumber())
                        .success(false)
                        .message("Allocation failed for order " + order.getOrderNumber() + ": " + ex.getMessage())
                        .build());
            }

            int successCount = (int) results.stream().filter(BulkOrderAllocationResult::isSuccess).count();
            int failureCount = results.size() - successCount;
            return BulkAllocationResponse.builder()
                    .totalOrders(results.size())
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .results(results)
                    .build();

        }

        Map<String, Integer> availableByBatchKey = new HashMap<>();

        List<OrderAllocationPlan> successfulPlans = new java.util.ArrayList<>();

        for (Order order : allocatableOrders) {

            String skuMrpKey = buildSkuMrpKey(order.getSkuCode(), order.getMrp());

            List<InventoryBatchResponse> batches = inventoryBySkuMrp.getOrDefault(
                    skuMrpKey,
                    java.util.Collections.emptyList()
            );

            List<InventoryBatchResponse> validBatches = batches.stream()
//                    .filter(Objects::nonNull)
//                    .filter(batch -> batch.getBatchNo() != null && !batch.getBatchNo().isBlank())
//                    .filter(batch -> batch.getExpiryDate() != null && batch.getExpiryDate().isAfter(java.time.LocalDate.now()))
//                    .filter(batch -> batch.getAvailableQty() != null && batch.getAvailableQty() > 0)
                    .sorted(Comparator.comparing(InventoryBatchResponse::getExpiryDate))
                    .toList();

            if (validBatches.isEmpty()) {
                results.add(BulkOrderAllocationResult.builder()
                        .orderNumber(order.getOrderNumber())
                        .success(false)
                        .message("Allocation failed for order " + order.getOrderNumber() + ": No valid inventory available")
                        .build());
                continue;
            }

            int remainingQty = order.getRequestedQty() - order.getAllocatedQty();
            int totalAllocatedNow = 0;

            List<BatchAllocationDetail> allocationDetails = new java.util.ArrayList<>();

            for (InventoryBatchResponse batch : validBatches) {

                if (remainingQty <= 0)
                    break;

                String batchKey = buildBatchKey(order.getSkuCode(), order.getMrp(), batch.getBatchNo());
                int available = availableByBatchKey.getOrDefault(batchKey, batch.getQuantity());

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

                availableByBatchKey.put(batchKey, available - allocateQty);

                remainingQty -= allocateQty;
                totalAllocatedNow += allocateQty;
            }

            if (totalAllocatedNow == 0) {
                results.add(BulkOrderAllocationResult.builder()
                        .orderNumber(order.getOrderNumber())
                        .success(false)
                        .message("Allocation failed for order " + order.getOrderNumber() + ": Insufficient stock for allocation")
                        .build());
                continue;
            }

            List<InventoryDeductRequest> deductRequests =
                    allocationDetails.stream()
                            .map(detail -> InventoryDeductRequest.builder()
                                    .sku(order.getSkuCode())
                                    .mrp(order.getMrp())
                                    .batchNo(detail.getBatchNo())
                                    .qty(detail.getAllocatedQty())
                                    .build())
                            .toList();

            successfulPlans.add(new OrderAllocationPlan(order, allocationDetails, deductRequests, totalAllocatedNow));
        }

        if (successfulPlans.isEmpty()) {
            int successCount = (int) results.stream().filter(BulkOrderAllocationResult::isSuccess).count();
            int failureCount = results.size() - successCount;
            return BulkAllocationResponse.builder()
                    .totalOrders(results.size())
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .results(results)
                    .build();
        }

//        List<InventoryOrderDeductPlan> deductPlans = successfulPlans.stream()
//                .map(plan -> InventoryOrderDeductPlan.builder()
////                        .orderNumber(plan.order().getOrderNumber())
//                        .skuCode(plan.order().getSkuCode())
//                        .mrp(plan.order().getMrp())
////                        .requestedQty(plan.order().getRequestedQty())
////                        .alreadyAllocatedQty(plan.order().getAllocatedQty())
//                        .allocations(plan.deductRequests())
//                        .build())
//                .toList();

        List<InventoryDeductRequest> deductAllocations = successfulPlans.stream()
                .flatMap(plan -> plan.deductRequests().stream())
                .toList();

        try {
            inventoryClient.deductInventoryBulk(deductAllocations);
        } catch (InventoryServiceException ex) {
            for (OrderAllocationPlan plan : successfulPlans) {
                results.add(BulkOrderAllocationResult.builder()
                        .orderNumber(plan.order().getOrderNumber())
                        .success(false)
                        .message("Allocation failed for order " + plan.order().getOrderNumber() + ": " + ex.getMessage())
                        .build());
            }

            int successCount = (int) results.stream().filter(BulkOrderAllocationResult::isSuccess).count();
            int failureCount = results.size() - successCount;

            return BulkAllocationResponse.builder()
                    .totalOrders(results.size())
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .results(results)
                    .build();
        }

        for (OrderAllocationPlan plan : successfulPlans) {
            try {
                AllocationResponse allocation = persistAllocationForOrder(plan);
                results.add(BulkOrderAllocationResult.builder()
                        .orderNumber(plan.order().getOrderNumber())
                        .success(true)
                        .message("Allocation completed")
                        .allocation(allocation)
                        .build());
            } catch (Exception ex) {
                results.add(BulkOrderAllocationResult.builder()
                        .orderNumber(plan.order().getOrderNumber())
                        .success(false)
                        .message("Allocation failed for order " + plan.order().getOrderNumber() + ": " + ex.getMessage())
                        .build());
            }
        }

        int successCount = (int) results.stream().filter(BulkOrderAllocationResult::isSuccess).count();
        int failureCount = results.size() - successCount;

        return BulkAllocationResponse.builder()
                .totalOrders(results.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .results(results)
                .build();
    }

    private AllocationResponse allocateOrderInternal(String orderNumber) {

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new BusinessException("Order not found"));

        if (order.getStatus() == OrderStatus.COMPLETED)
            throw new AllocationException("Order already completed");

        int remainingQty = order.getRequestedQty() - order.getAllocatedQty();

        if (remainingQty <= 0)
            throw new AllocationException("Order already fully allocated");

//        List<InventoryBatchResponse> batches = inventoryClient.getBatchesBySkuAndMrp(order.getSkuCode(), order.getMrp());

        List<InventoryBatchResponse> batches;
        try {
            batches = inventoryClient.getBatchesBySkuAndMrp(order.getSkuCode(), order.getMrp());
        } catch (InventoryServiceException ex) {
            throw new AllocationException(ex.getMessage());
        }

        List<InventoryBatchResponse> validBatches = batches.stream()
//                .filter(Objects::nonNull)
//                .filter(batch -> batch.getBatchNo() != null && !batch.getBatchNo().isBlank())
//                .filter(batch -> batch.getExpiryDate() != null && batch.getExpiryDate().isAfter(java.time.LocalDate.now()))
//                .filter(batch -> batch.getAvailableQty() != null && batch.getAvailableQty() > 0)
                .sorted(Comparator.comparing(InventoryBatchResponse::getExpiryDate))
                .toList();

        if (validBatches.isEmpty())
            throw new AllocationException("No valid inventory available");

        int totalAllocatedNow = 0;

        List<BatchAllocationDetail> allocationDetails = new java.util.ArrayList<>();

        for (InventoryBatchResponse batch: validBatches) {

            if (remainingQty <= 0)
                break;

            int available = batch.getQuantity();

            if (available <= 0)
                continue;

            int allocateQty = Math.min(remainingQty, available);

            allocationDetails.add(
                    BatchAllocationDetail.builder()
                            .batchNo(batch.getBatchNo())
                            .quantity(allocateQty)
//                            .expiryDate(batch.getExpiryDate())
//                            .mrp(batch.getMrp())
//                            .allocatedQty(allocateQty)
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
//                        .skuCode(order.getSkuCode())
                                .sku(order.getSkuCode())
                                .mrp(order.getMrp())
                                .batchNo(detail.getBatchNo())
//                        .mrp(order.getMrp())
                                .qty(detail.getQuantity())
                                .build()
                )
                .toList();

//        inventoryClient.deductInventoryBulk(
//                List.of(
//                        InventoryOrderDeductPlan.builder()
//                                .skuCode(order.getSkuCode())
//                                .mrp((order.getMrp()))
//                                .allocations(deductRequests)
//                                .build()
//                )
//////                order.getOrderNumber(),
////                order.getSkuCode(),
////                order.getMrp(),
//////                order.getRequestedQty(),
//////                order.getAllocatedQty(),
////                deductRequests
//        );

        try {
            inventoryClient.deductInventoryBulk(deductRequests);
        } catch (InventoryServiceException ex) {
            throw new AllocationException(ex.getMessage());
        }

        for (BatchAllocationDetail detail : allocationDetails) {

            OrderAllocation allocation = OrderAllocation.builder()
                    .order(order)
                    .skuCode(order.getSkuCode())
                    .batchNo(detail.getBatchNo())
//                    .expiryDate(detail.getExpiryDate())
//                    .mrp(detail.getMrp())
//                    .allocatedQty(detail.getAllocatedQty())
                    .allocatedQty(detail.getQuantity())
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

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    protected AllocationResponse persistAllocationForOrder(OrderAllocationPlan plan) {

        Order order = plan.order();

        for (BatchAllocationDetail detail : plan.allocationDetails()) {

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

        order.setAllocatedQty(order.getAllocatedQty() + plan.totalAllocatedNow());

        if (order.getAllocatedQty().equals(order.getRequestedQty()))
            order.setStatus(OrderStatus.COMPLETED);
        else if (order.getAllocatedQty() > 0)
            order.setStatus(OrderStatus.PARTIAL);

        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        return AllocationResponse.builder()
                .orderNumber(order.getOrderNumber())
                .requestedQty(order.getRequestedQty())
                .allocatedQty(order.getAllocatedQty())
                .status(order.getStatus())
                .allocations(plan.allocationDetails())
                .build();
    }

    private String buildSkuMrpKey(String skuCode, Double mrp) {
        return skuCode + "||" + mrp;
    }

    private String buildBatchKey(String skuCode, Double mrp, String batchNo) {
        return skuCode + "||" + mrp + "||" + batchNo;
    }

    private record OrderAllocationPlan(
            Order order,
            List<BatchAllocationDetail> allocationDetails,
            List<InventoryDeductRequest> deductRequests,
            int totalAllocatedNow
    ) {
    }

    @Override
    public DashboardSummaryResponse getDashboardSummary() {

//        List<Order> orderList = orderRepository.findAll();
//
//
//
//        long pending =0, partial = 0, completed = 0;
//        for(Order order: orderList){
//            if(PENDING.equals(order.getStatus())) {
//                pending++;
//            }
//
//
//        }

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
                .mrp(order.getMrp())
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
                                .date(((java.time.LocalDateTime) obj[0]).toLocalDate())
                                .orderCount((Long) obj[1])
                                .build())
                        .toList();

        List<Object[]> allocationRaw =
                orderRepository.getDailyAllocation(last7Days);

//        List<Order> orderList = orderRepository.findByCre



        List<DailyAllocationData> dailyAllocations =
                allocationRaw.stream()
                        .map(obj -> DailyAllocationData.builder()
                                .date(((java.time.LocalDateTime) obj[0]).toLocalDate())
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








