package com.addverb.outbound_service.repository;

import com.addverb.outbound_service.entity.Order;
import com.addverb.outbound_service.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderNumber(String orderNumber);

    @Override
    Page<Order> findAll(Pageable pageable);

    long countByStatus(OrderStatus status);

    long countByCreatedAtBetween(
            LocalDateTime start,
            LocalDateTime end
    );

    @Query("SELECT COALESCE(SUM(o.allocatedQty),0) FROM Order o")
    Long getTotalAllocatedQuantity();

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> getStatusDistribution();

    @Query("""
    SELECT DATE(o.createdAt), COUNT(o)
    FROM Order o
    WHERE o.createdAt >= :startDate
    GROUP BY DATE(o.createdAt)
    ORDER BY DATE(o.createdAt)
    """)
    List<Object[]> getDailyOrders(LocalDateTime startDate);

    @Query("""
    SELECT DATE(o.createdAt), SUM(o.allocatedQty)
    FROM Order o
    WHERE o.createdAt >= :startDate
    GROUP BY DATE(o.createdAt)
    ORDER BY DATE(o.createdAt)
    """)
    List<Object[]> getDailyAllocation(LocalDateTime startDate);

}





