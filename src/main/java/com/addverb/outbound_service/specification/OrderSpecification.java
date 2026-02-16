package com.addverb.outbound_service.specification;

import com.addverb.outbound_service.entity.Order;
import com.addverb.outbound_service.enums.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;


public class OrderSpecification {

    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) ->
                status == null ? null :
                cb.equal(root.get("status"), status);
    }

    public static Specification<Order> hasSkuCode(String skuCode) {
        return (root, query, cb) ->
                (skuCode == null || skuCode.isBlank()) ? null :
                        cb.equal(root.get("skuCode"), skuCode);
    }

    public static Specification<Order> createdAfter(LocalDateTime fromDate) {
        return (root, query, cb) ->
                fromDate == null ? null :
                        cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate);
    }

    public static Specification<Order> createdBefore(LocalDateTime toDate) {
        return (root, query, cb) ->
                toDate == null ? null :
                        cb.lessThanOrEqualTo(root.get("createdAt"), toDate);
    }

}



