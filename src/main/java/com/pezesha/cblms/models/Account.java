package com.pezesha.cblms.models;

import com.pezesha.cblms.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author AOmar
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Table(name = "tb_accounts")
public class Account extends BaseEntity{

    @Id
    private Long id;
    private String code;
    private String name;
    private String type;
    private String currency;
    private Long parentAccountId;
    private BigDecimal currentBalance = BigDecimal.ZERO;
    private boolean isActive = false;
    private boolean hasTransaction = false;
    private boolean isDeleted = false;
}
