package com.pezesha.cblms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author AOmar
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoanDisbursementResponse {

    private String loanId;

    private String borrowerId;

    private BigDecimal principalAmount;

    private BigDecimal interestRate;

    private BigDecimal originationFee;

    private BigDecimal netDisbursement;

    private String currency;

    private Instant disbursementDate;

    private Instant maturityDate;

    private String status;

    private BigDecimal outstandingPrincipal;

    private BigDecimal outstandingInterest;


}