package com.payment.router.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Routed payment message forwarded to the payment engine.
 * Wraps the original PAIN 001 with routing decision metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutedPaymentMessage {

    @JsonProperty("originalMessageId")
    private String originalMessageId;

    @JsonProperty("originalPaymentId")
    private String originalPaymentId;

    @JsonProperty("paymentType")
    private String paymentType; // "Instant Payment" or "Ach Payment"

    @JsonProperty("abaRoutingNumber")
    private String abaRoutingNumber;

    @JsonProperty("routingDecisionReason")
    private String routingDecisionReason;

    @JsonProperty("originalMessage")
    private Pain001Message originalMessage;
}
