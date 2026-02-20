package com.addverb.outbound_service.inventory;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InventoryBatchesBulkRequest {
    private List<InventorySkuMrpRequest> queries;
}


