package com.pezesha.cblms.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author AOmar
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table("tb_journal_entries")
public class JournalEntry {

    @Id
    private Long id;

    private Long transactionId;

    private Long accountId;

    private String accountCode;

    private String accountName;

    private String accountType;

    private BigDecimal debit = BigDecimal.ZERO;

    private BigDecimal credit = BigDecimal.ZERO;

    private Instant transactionDate;
}