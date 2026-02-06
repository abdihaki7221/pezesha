package com.pezesha.cblms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author AOmar
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FetchAllAccountsResponse {
   private List<FetchAccountDetailsResponse> accounts;
    private int currentPage;
    private int pageSize;
    private int totalPages;
    private long totalElements;
}
