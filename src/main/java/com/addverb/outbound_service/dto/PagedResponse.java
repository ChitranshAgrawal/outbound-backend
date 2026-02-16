package com.addverb.outbound_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;


@Data
@Builder
public class PagedResponse<T> {
    private List<T> content;
    private int currentPage;
    private int totalPages;
    private long totalElements;
}



