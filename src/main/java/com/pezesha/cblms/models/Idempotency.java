package com.pezesha.cblms.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * @author AOmar
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table("tb_idempotency")
public class Idempotency {

    @Id
    private Long id;

    private String key;

    private Long transactionId;

    private Instant createdAt;
}