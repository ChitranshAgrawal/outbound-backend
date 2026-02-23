package com.addverb.outbound_service.inventory;

import com.addverb.outbound_service.exception.BusinessException;
import com.addverb.outbound_service.exception.InventoryServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

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

        List<InventorySkuMrpRequest> bulkQueries = queries.stream()
                .filter(query -> query != null && query.skuCode() != null && !query.skuCode().isBlank() && query.mrp() != null)
                .collect(Collectors.toMap(
                        query -> buildKey(query.skuCode(), query.mrp()),
                        query -> InventorySkuMrpRequest.builder()
                                .sku(query.skuCode())
                                .mrp(query.mrp())
                                .build(),
                        (existing, ignored) -> existing,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();

        if (bulkQueries.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            log.info("InventoryClient.getBatchesBySkuAndMrpBulk payload size={}", bulkQueries.size());
            String rawResponse = callAvailableEndpoint(bulkQueries);
            InventoryBatchesBySkuMrpResponse[] response = parseAvailableResponse(rawResponse);

            if (response == null)
                return Collections.emptyMap();

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

    private String callAvailableEndpoint(Object payload) {
        return webClient.post()
                .uri(INVENTORY_BASE_URL + "/available")
                .bodyValue(payload)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        error -> error.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new InventoryServiceException("Inventory fetch failed: " + body)))
                )
                .bodyToMono(String.class)
                .block();
    }


    private InventoryBatchesBySkuMrpResponse[] parseAvailableResponse(String rawResponse) {

        if (rawResponse == null || rawResponse.isBlank()) {
            return new InventoryBatchesBySkuMrpResponse[0];
        }

        try {
            JsonNode root = objectMapper.readTree(rawResponse);

            if (root.isArray()) {
                return objectMapper.treeToValue(root, InventoryBatchesBySkuMrpResponse[].class);
            }

            if (looksLikeSkuMrpResponse(root)) {
                return new InventoryBatchesBySkuMrpResponse[]{
                        objectMapper.treeToValue(root, InventoryBatchesBySkuMrpResponse.class)
                };
            }

            if (root.has("success") && !root.path("success").asBoolean(true)) {
                String message = root.path("message").asText("Inventory fetch failed");
                throw new InventoryServiceException("Inventory fetch failed: " + message);
            }

            JsonNode dataNode = root.get("data");
            if (dataNode != null) {
                if (dataNode.isObject()) {
                    InventoryBatchesBySkuMrpResponse[] mapBased = parseSkuMrpMapData(dataNode);
                    if (mapBased.length > 0) {
                        return mapBased;
                    }
                }

                if (dataNode.isArray()) {
                    if (dataNode.size() > 0 && dataNode.get(0).isObject() && dataNode.get(0).has("sku") && dataNode.get(0).has("batchNo")) {
                        return mapFlatBatchRows(dataNode);
                    }
                    return objectMapper.treeToValue(dataNode, InventoryBatchesBySkuMrpResponse[].class);
                }
                if (looksLikeSkuMrpResponse(dataNode)) {
                    return new InventoryBatchesBySkuMrpResponse[]{
                            objectMapper.treeToValue(dataNode, InventoryBatchesBySkuMrpResponse.class)
                    };
                }

            }

            throw new InventoryServiceException("Inventory fetch failed: Unexpected /available response shape -> " + rawResponse);
        } catch (InventoryServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InventoryServiceException("Inventory response parsing failed: " + ex.getMessage());
        }
    }

    private InventoryBatchesBySkuMrpResponse[] parseSkuMrpMapData(JsonNode dataNode) {
        List<InventoryBatchesBySkuMrpResponse> mapped = new ArrayList<>();

        dataNode.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            if (!value.isArray()) {
                return;
            }

            String[] parts = key.split("\\|\\|", 2);
            if (parts.length != 2) {
                return;
            }

            Double mrp;
            try {
                mrp = Double.parseDouble(parts[1]);
            } catch (NumberFormatException ex) {
                return;
            }

            List<InventoryBatchResponse> batches = new ArrayList<>();
            value.forEach(item -> {
                if (!item.isObject()) {
                    return;
                }

                InventoryBatchResponse batch = new InventoryBatchResponse();
                batch.setBatchNo(item.path("batchNo").isMissingNode() ? null : item.path("batchNo").asText(null));

                if (!item.path("mrp").isMissingNode() && !item.path("mrp").isNull()) {
                    batch.setMrp(item.path("mrp").asDouble());
                }

                if (!item.path("quantity").isMissingNode() && !item.path("quantity").isNull()) {
                    batch.setQuantity(item.path("quantity").asInt());
                }

                if (!item.path("expiryDate").isMissingNode() && !item.path("expiryDate").isNull()) {
                    try {
                        batch.setExpiryDate(java.time.LocalDate.parse(item.path("expiryDate").asText()));
                    } catch (Exception ignored) {
                    }
                }

                if (batch.getBatchNo() != null) {
                    batches.add(batch);
                }
            });

            InventoryBatchesBySkuMrpResponse row = new InventoryBatchesBySkuMrpResponse();
            row.setSku(parts[0]);
            row.setMrp(mrp);
            row.setBatches(batches);
            mapped.add(row);
        });

        return mapped.toArray(new InventoryBatchesBySkuMrpResponse[0]);
    }

    private InventoryBatchesBySkuMrpResponse[] mapFlatBatchRows(JsonNode rowsNode) {
        List<InventoryFlatBatchRow> rows = Arrays.asList(objectMapper.convertValue(rowsNode, InventoryFlatBatchRow[].class));

        Map<String, List<InventoryBatchResponse>> grouped = new java.util.LinkedHashMap<>();
        Map<String, Double> mrpByKey = new java.util.LinkedHashMap<>();

        for (InventoryFlatBatchRow row : rows) {
            if (row == null || row.sku == null || row.sku.isBlank() || row.mrp == null) {
                continue;
            }

            String key = buildKey(row.sku, row.mrp);
            mrpByKey.putIfAbsent(key, row.mrp);
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>());

            if (row.batchNo != null && row.quantity != null) {
                InventoryBatchResponse batch = new InventoryBatchResponse();
                batch.setBatchNo(row.batchNo);
                batch.setMrp(row.mrp);
                batch.setQuantity(row.quantity);
                batch.setExpiryDate(row.expiryDate);
                grouped.get(key).add(batch);
            }
        }

        List<InventoryBatchesBySkuMrpResponse> mapped = new ArrayList<>();
        grouped.forEach((key, batches) -> {
            String[] parts = key.split("\\|\\|", 2);
            if (parts.length != 2) {
                return;
            }

            InventoryBatchesBySkuMrpResponse row = new InventoryBatchesBySkuMrpResponse();
            row.setSku(parts[0]);
            row.setMrp(mrpByKey.get(key));
            row.setBatches(batches);
            mapped.add(row);
        });

        return mapped.toArray(new InventoryBatchesBySkuMrpResponse[0]);
    }

    private static class InventoryFlatBatchRow {
        public String sku;
        public Double mrp;
        public String batchNo;
        public java.time.LocalDate expiryDate;
        public Integer quantity;
    }

    private boolean looksLikeSkuMrpResponse(JsonNode node) {
        return node != null && node.isObject() && node.has("sku") && node.has("mrp") && node.has("batches");
    }


    public void deductInventoryBulk(
            List<InventoryDeductRequest> items
    ) {

        InventoryBulkOrdersDeductRequest request = InventoryBulkOrdersDeductRequest.builder()
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
        return skuCode == null ? "||" + mrp : skuCode.trim().toLowerCase() + "||" + mrp;
    }

}

