package com.addverb.outbound_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;


@Data
public class CreateOrderRequest {

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "SKU code is required")
    private String skuCode;

    @NotNull(message = "MRP is required")
    @Positive(message = "MRP must be greater than 0")
    private Double mrp;

    @NotNull(message = "Requested quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer requestedQty;

}




