package com.addverb.outbound_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;


@Data
@Builder
public class DailyOrderData {
    private LocalDate date;
    private Long orderCount;
}
