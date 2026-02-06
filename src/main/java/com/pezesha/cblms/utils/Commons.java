package com.pezesha.cblms.utils;

import java.security.SecureRandom;

/**
 * @author AOmar
 */
public class Commons {
    private static final SecureRandom random = new SecureRandom();

    public static String createUniqueAccountId() {
        long number = 1_000_000_000L +
                (long)(random.nextDouble() * 9_000_000_000L);
        return String.valueOf(number);
    }
}
