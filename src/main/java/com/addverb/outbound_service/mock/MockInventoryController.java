package com.addverb.outbound_service.mock;

import com.addverb.outbound_service.inventory.InventoryBatchResponse;
import com.addverb.outbound_service.inventory.InventoryDeductRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/api/inventory")
public class MockInventoryController {

    @GetMapping("/batches/{sku}")
    public List<InventoryBatchResponse> getBatches(@PathVariable String sku) {

        InventoryBatchResponse b1 = new InventoryBatchResponse();
        b1.setBatchNo("BATCH-1");
        b1.setExpiryDate(LocalDate.now().plusDays(10));
        b1.setMrp(120.0);
        b1.setAvailableQty(5);

        InventoryBatchResponse b2 = new InventoryBatchResponse();
        b2.setBatchNo("BATCH-2");
        b2.setExpiryDate(LocalDate.now().plusDays(30));
        b2.setMrp(120.0);
        b2.setAvailableQty(10);

        return List.of(b1, b2);
    }

    @PostMapping("/deduct")
    public void deductInventory(@RequestBody InventoryDeductRequest request) {
        if (request.getQuantity() > 10)
            throw new RuntimeException("Not enough stock");
    }

}





