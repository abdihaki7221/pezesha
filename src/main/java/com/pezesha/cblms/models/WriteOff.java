package com.pezesha.cblms.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Entity model for loan write-offs
 *
 * @author AOmar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("write_offs")
public class WriteOff {

    @Id
    private Long id;

    @Column("write_off_id")
    private String writeOffId;

    @Column("loan_id")
    private String loanId;

    @Column("write_off_date")
    private Instant writeOffDate;

    @Column("write_off_type")
    private String writeOffType;

    @Column("principal_written_off")
    private BigDecimal principalWrittenOff;

    @Column("interest_written_off")
    private BigDecimal interestWrittenOff;

    @Column("total_written_off")
    private BigDecimal totalWrittenOff;

    @Column("reason")
    private String reason;

    @Column("transaction_id")
    private String transactionId;

    @Column("created_at")
    private LocalDateTime createdAt;
}