package com.addverb.outbound_service.inventory;

import com.addverb.outbound_service.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final WebClient webClient;

    public final String INVENTORY_BASE_URL = "http://localhost:8080/api/inventory";

    public List<InventoryBatchResponse> getBatchesBySku(String skuCode) {
        InventoryBatchResponse[] response = webClient.get()
                .uri(INVENTORY_BASE_URL + "/batches/{sku}", skuCode)
                .retrieve()
                .bodyToMono(InventoryBatchResponse[].class)
                .block();

        return response == null ? Collections.emptyList() : Arrays.asList(response);
    }

//    public void deductInventory(String skuCode, String batchNo, Integer quantity) {
//
//        InventoryDeductRequest request = InventoryDeductRequest.builder()
//                .skuCode(skuCode)
//                .batchNo(batchNo)
//                .quantity(quantity)
//                .build();
//
//        webClient.post()
//                .uri(INVENTORY_BASE_URL + "/deduct")
//                .bodyValue(request)
//                .retrieve()
//                .onStatus(status -> status.isError(),
//                        response -> response.bodyToMono(String.class)
//                                .map(error -> new BusinessException(
//                                        "Inventory Deduction Failed for Batch" + batchNo + ": " + error
//                                )))
//                .toBodilessEntity()
//                .block();
//    }

    public void deductInventoryBulk(
            String orderNumber,
            String skuCode,
            Integer requestedQty,
            Integer alreadyAllocatedQty,
            List<InventoryDeductRequest> allocations
    ) {

        InventoryBulkDeductRequest request = InventoryBulkDeductRequest.builder()
                .orderNumber(orderNumber)
                .skuCode(skuCode)
                .requestedQty(requestedQty)
                .alreadyAllocatedQty(alreadyAllocatedQty)
                .allocations(allocations)
                .build();

        webClient.post()
                .uri(INVENTORY_BASE_URL + "/deduct/bulk")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .map(error -> new BusinessException("Inventory bulk deduction failed: " + error))
                )
                .toBodilessEntity()
                .block();
    }

}



