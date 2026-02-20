//package com.addverb.outbound_service.mock;
//
//import com.addverb.outbound_service.inventory.InventoryBatchResponse;
//import com.addverb.outbound_service.inventory.InventoryBulkDeductRequest;
//import com.addverb.outbound_service.inventory.InventoryDeductRequest;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.LocalDate;
//import java.util.List;
//
//
//@RestController
//@RequestMapping("/api/inventory")
//public class MockInventoryController {
//
//    @GetMapping("/batches")
//    public List<InventoryBatchResponse> getBatches(@RequestParam String skuCode, @RequestParam Double mrp) {
//
//        InventoryBatchResponse b1 = new InventoryBatchResponse();
//        b1.setBatchNo("BATCH-1");
//        b1.setExpiryDate(LocalDate.now().plusDays(10));
//        b1.setMrp(mrp);
//        b1.setQuantity(5);
//
//        InventoryBatchResponse b2 = new InventoryBatchResponse();
//        b2.setBatchNo("BATCH-2");
//        b2.setExpiryDate(LocalDate.now().plusDays(30));
//        b2.setMrp(mrp);
//        b2.setQuantity(10);
//
//        return List.of(b1, b2);
//    }
//
//    @PostMapping("/deduct/bulk")
//    public void deductInventoryBulk(@RequestBody InventoryBulkDeductRequest request) {
//
//        if (request.getOrderNumber() == null || request.getOrderNumber().isBlank())
//            throw new RuntimeException("Order number is required");
//
//        if (request.getSkuCode() == null || request.getSkuCode().isBlank())
//            throw new RuntimeException("SKU code is required");
//
//        if (request.getAllocations() == null || request.getAllocations().isEmpty())
//            throw new RuntimeException("No allocations supplied");
//
//        boolean invalid = request.getAllocations().stream()
//                .anyMatch(allocation -> allocation.getQuantity() == null || allocation.getQuantity() <= 0 || allocation.getQuantity() > 10);
//
//        if (invalid)
//            throw new RuntimeException("Not enough stock");
//    }
//
//    @PostMapping("/deduct")
//    public void deductInventory(@RequestBody InventoryDeductRequest request) {
//        if (request.getQuantity() > 10)
//            throw new RuntimeException("Not enough stock");
//    }
//
//}
//
//
//
//
//
