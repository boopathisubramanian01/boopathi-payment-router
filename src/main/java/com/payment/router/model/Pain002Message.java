package com.payment.router.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ISO 20022 PAIN 002 - Payment Status Report
 * Generated when a payment is rejected due to validation failure.
 * This router currently emits ISO status RJCT on validation failures.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pain002Message {

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("creationDateTime")
    private LocalDateTime creationDateTime;

    @JsonProperty("version")
    @Builder.Default
    private String version = "pain.002.003.03";

    @JsonProperty("originalMessageId")
    private String originalMessageId;

    @JsonProperty("originalPaymentId")
    private String originalPaymentId;

    @JsonProperty("transactionStatus")
    private String transactionStatus; // RJCT = Rejected

    @JsonProperty("statusReasonCode")
    private String statusReasonCode; // e.g. AM01, AC01, FF01

    @JsonProperty("statusReasonDescription")
    private String statusReasonDescription;

    @JsonProperty("validationErrors")
    private List<ValidationError> validationErrors;

    @JsonProperty("rejectedAt")
    private LocalDateTime rejectedAt;

    @JsonProperty("originalDebtorAccount")
    private String originalDebtorAccount;

    @JsonProperty("originalCreditorAccount")
    private String originalCreditorAccount;

    @JsonProperty("originalAmount")
    private String originalAmount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        @JsonProperty("field")
        private String field;
        @JsonProperty("rejectionCode")
        private String rejectionCode;
        @JsonProperty("description")
        private String description;
    }

    // ISO 20022 standard rejection reason codes
    public static final String CODE_MISSING_DEBTOR_ACCOUNT  = "AC01"; // Incorrect Account Number
    public static final String CODE_MISSING_CREDITOR_ACCOUNT = "AC03"; // Invalid Creditor Account Number
    public static final String CODE_MISSING_ROUTING_NUMBER  = "RC01"; // Bank Identifier Incorrect
    public static final String CODE_INVALID_AMOUNT          = "AM01"; // Zero Amount
    public static final String CODE_MISSING_AMOUNT          = "AM02"; // Not Allowed Amount
    public static final String STATUS_RCVD                  = "RCVD";
    public static final String STATUS_ACTC                  = "ACTC";
    public static final String STATUS_ACCP                  = "ACCP";
    public static final String STATUS_ACSP                  = "ACSP";
    public static final String STATUS_PDNG                  = "PDNG";
    public static final String STATUS_RJCT                  = "RJCT";
}
