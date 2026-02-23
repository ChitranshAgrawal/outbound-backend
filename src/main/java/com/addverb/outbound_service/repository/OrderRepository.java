package com.addverb.outbound_service.repository;

import com.addverb.outbound_service.entity.Order;
import com.addverb.outbound_service.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderNumber(String orderNumber);

//    List<Order> findByOrderNumberIn(List<String> orderNumbers);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderNumber IN :orderNumbers")
    List<Order> findByOrderNumberInForUpdate(@Param("orderNumbers") List<String> orderNumbers);

    @Override
    Page<Order> findAll(Pageable pageable);

    List<Order> findByCreatedAtBetween(LocalDateTime fromDate, LocalDateTime toDate, Sort sort);

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
    SELECT o.createdAt, COUNT(o)
    FROM Order o
    WHERE o.createdAt >= :startDate
    GROUP BY o.createdAt
    ORDER BY o.createdAt
    """)
    List<Object[]> getDailyOrders(@Param("startDate") LocalDateTime startDate);

    @Query("""
    SELECT o.createdAt, SUM(o.allocatedQty)
    FROM Order o
    WHERE o.createdAt >= :startDate
    GROUP BY o.createdAt
    ORDER BY o.createdAt
    """)
    List<Object[]> getDailyAllocation(@Param("startDate") LocalDateTime startDate);

}




