package com.addverb.outbound_service.repository;

import com.addverb.outbound_service.entity.OrderAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderAllocationRepository extends JpaRepository<OrderAllocation, Long> {
}


