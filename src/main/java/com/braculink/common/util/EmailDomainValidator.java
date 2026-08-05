package com.braculink.common.util;

public final class EmailDomainValidator {

    private static final String ALLOWED_DOMAIN = "@g.bracu.ac.bd";

    private EmailDomainValidator() {
    }

    public static boolean isValidBracuEmail(String email) {
        return email != null && email.toLowerCase().endsWith(ALLOWED_DOMAIN);
    }
}
