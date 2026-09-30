package com.roommate.management.exception;

/** Thrown when we deliberately need the caller to know an email genuinely failed to send -
 *  distinct from "this address isn't registered" (which stays silent, by design, to avoid
 *  account enumeration). Maps to 503: a transient, operational failure, not tied to any
 *  specific account. */
public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException(String message) {
        super(message);
    }
}
