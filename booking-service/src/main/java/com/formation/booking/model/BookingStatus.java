package com.formation.booking.model;

/**
 * Cycle de vie d'une reservation.
 *
 * <ul>
 *   <li><b>PENDING_PAYMENT</b> : places reservees mais paiement non effectue (deadline = +1h).</li>
 *   <li><b>CONFIRMED</b>      : paiement accepte.</li>
 *   <li><b>CANCELLED</b>      : annulee (par l'utilisateur ou expiration du paiement).</li>
 *   <li><b>COMPLETED</b>      : cours passe, reservation soldée.</li>
 *   <li><b>NO_SHOW</b>       : utilisateur absent le jour du cours.</li>
 * </ul>
 */
public enum BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
    NO_SHOW
}
