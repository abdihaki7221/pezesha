package com.pezesha.cblms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author AOmar
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Pagination {
    private int currentPage;
    private int pageSize;
    private int totalPages;
    private long totalElements;
}