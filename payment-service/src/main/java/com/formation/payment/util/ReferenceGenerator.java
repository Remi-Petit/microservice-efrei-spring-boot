package com.formation.payment.util;

import java.util.UUID;

/**
 * Genere des references uniques du type "PAY-XXXXX".
 */
public final class ReferenceGenerator {

    private ReferenceGenerator() {
    }

    public static String nextPaymentReference() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }
}
