package com.addverb.outbound_service.inventory;

import com.addverb.outbound_service.exception.BusinessException;
import com.addverb.outbound_service.exception.InventoryServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryClient {

    private final WebClient webClient;

    public final String INVENTORY_BASE_URL = "http://172.19.8.200:8080/api/inventory";

//    public List<InventoryBatchResponse> getBatchesBySkuAndMrp(String skuCode, Double mrp) {
//        InventoryBatchResponse[] response = webClient.get()
//                .uri(INVENTORY_BASE_URL + "/available?sku={skuCode}&mrp={mrp}", skuCode, mrp)
//                .retrieve()
////                .bodyToMono(InventoryBatchResponse[].class)
//                .onStatus(
//                        status -> status.isError(),
//                        res -> res.bodyToMono(String.class)
//                                .map(error -> new BusinessException(
//                                        "Inventory Deduction Failed: " + error
//                                ))
//                )
//                .bodyToMono(InventoryBatchResponse[].class)
//                .block();
//
//        return response == null ? Collections.emptyList() : Arrays.asList(response);
//    }

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

    public List<InventoryBatchResponse> getBatchesBySkuAndMrp(String skuCode, Double mrp) {

        Map<String, List<InventoryBatchResponse>> response = getBatchesBySkuAndMrpBulk(List.of(new OrderInventoryQuery(skuCode, mrp)));

        return response.getOrDefault(buildKey(skuCode, mrp), Collections.emptyList());
    }

    public Map<String, List<InventoryBatchResponse>> getBatchesBySkuAndMrpBulk(List<OrderInventoryQuery> queries) {

        if (queries == null || queries.isEmpty()) {
            return Collections.emptyMap();
        }

        System.out.println(queries);

        List<InventorySkuMrpRequest> bulkQueries = queries.stream()
                .map(query -> InventorySkuMrpRequest.builder()
                    .sku(query.skuCode)
                    .mrp(query.mrp)
                    .build())
                .toList();

        System.out.println(bulkQueries);

        System.out.println(InventoryBatchesBySkuMrpResponse.class);

        try {
            log.info("InventoryClient.getBatchesBySkuAndMrpBulk: " + bulkQueries);
            InventoryBatchesBySkuMrpResponse[] response = webClient.post()
                    .uri(INVENTORY_BASE_URL + "/available")
                    .bodyValue(bulkQueries)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            error -> error.bodyToMono(String.class)
                                    .map(body -> new InventoryServiceException("Inventory fetch failed: " + body))
                    )
                    .bodyToMono(InventoryBatchesBySkuMrpResponse[].class)
                    .block();

            if (response == null)
                return Collections.emptyMap();

            System.out.println(response);

            return Arrays.stream(response)
                    .filter(item -> item != null && item.getSku() != null && item.getMrp() != null)
                    .collect(Collectors.toMap(
                            item -> buildKey(item.getSku(), item.getMrp()),
                            item -> item.getBatches() != null ? item.getBatches() : Collections.emptyList(),
                            (existing, ignored) -> existing
                    ));
        } catch (WebClientException ex) {
            throw new InventoryServiceException("Inventory service request failed: " + ex.getMessage());
        }
    }

    public void deductInventoryBulk(
////            String orderNumber,
//            String sku,
//            Double mrp,
////            Integer requestedQty,
////            Integer alreadyAllocatedQty,
//            List<InventoryDeductRequest> batches
            List<InventoryDeductRequest> items
    ) {

        InventoryBulkOrdersDeductRequest request = InventoryBulkOrdersDeductRequest.builder()
//                .orderNumber(orderNumber)
//                .sku(sku)
//                .mrp(mrp)
//                .operation("DEDUCT")
////                .requestedQty(requestedQty)
////                .alreadyAllocatedQty(alreadyAllocatedQty)
//                .batches(batches)
                .items(items)
                .operation("DEDUCT")
                .build();

        try {
            webClient.post()
                    .uri(INVENTORY_BASE_URL + "/save")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            response -> response.bodyToMono(String.class)
                                    .map(error -> new InventoryServiceException("Inventory bulk deduction failed: " + error))
                    )
                    .toBodilessEntity()
                    .block();
        } catch (WebClientException ex) {
            throw new InventoryServiceException("Inventory service request failed: " + ex.getMessage());
        }
    }

    public record OrderInventoryQuery(String skuCode, Double mrp) {}

    private String buildKey(String skuCode, Double mrp) {
        return skuCode + "||" + mrp;
    }

}



