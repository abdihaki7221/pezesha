package com.pezesha.cblms.exceptions;

/**
 * @author AOmar
 */
public class ServiceValidation extends RuntimeException {
    public ServiceValidation(String message) {
        super(message);
    }
}
