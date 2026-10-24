package com.pedidai.api.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Avís de Stripe ja processat. Stripe pot enviar el mateix avís més d'una vegada:
 * si l'id ja hi és, no es torna a aplicar.
 */
@Entity
@Table(name = "stripe_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StripeEvent {

    @Id
    private String id;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
}
