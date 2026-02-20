package com.addverb.outbound_service.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkAllocateOrdersRequest {

    @NotEmpty(message = "Order numbers are required")
    private List<String> orderNumbers;
}

