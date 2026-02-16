package com.addverb.outbound_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;


@Data
@Builder
public class DailyAllocationData {
    private LocalDate date;
    private Long allocatedQty;
}
