package com.formation.booking.util;

import java.util.UUID;

/**
 * Genere des references uniques du type "BK-XXXXX".
 */
public final class ReferenceGenerator {

    private ReferenceGenerator() {
    }

    public static String nextBookingReference() {
        return "BK-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }
}
