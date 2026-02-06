package com.pezesha.cblms.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * @author AOmar
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table("tb_transactions")
public class Transaction extends BaseEntity {

    @Id
    private Long id;

    private String transactionId;

    private String idempotencyKey;

    private String status;

    private Instant transactionDate;

    private String description;

    private String currency;

    private BigDecimal totalDebit = BigDecimal.ZERO;

    private BigDecimal totalCredit = BigDecimal.ZERO;

    private Instant postedAt;

    private String reversedBy;

    private Long reversalTransactionId;

    private Instant reversalDate;

    private String reason;
}