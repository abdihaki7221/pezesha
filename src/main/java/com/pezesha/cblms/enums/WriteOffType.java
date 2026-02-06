package com.pezesha.cblms.enums;

/**
 * Enum for loan write-off types
 *
 * @author AOmar
 */
public enum WriteOffType {
    /**
     * Write off entire outstanding balance (principal + interest)
     */
    FULL,

    /**
     * Write off partial amount of outstanding balance
     */
    PARTIAL
}