package com.payment.router.service;

import com.payment.router.model.Pain001Message;
import com.payment.router.model.Pain002Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PaymentValidationService {

    public ValidationResult validate(Pain001Message message) {
        List<Pain002Message.ValidationError> errors = new ArrayList<>();
        Pain001Message.PaymentInformation pi = message.getPaymentInformation();
        Pain001Message.CreditTransferTransaction txn = (pi != null) ? pi.getCreditTransferTransaction() : null;

        log.info("[VALIDATOR] Starting mandatory field validation | messageId={}", message.getMessageId());

        // ── Debtor Account ──────────────────────────────────────────────────
        String debtorIban = (pi != null && pi.getDebtorAccount() != null) ? pi.getDebtorAccount().getIban() : null;
        if (isBlank(debtorIban)) {
            log.warn("[VALIDATOR] FAIL - Debtor account (IBAN) is missing or blank | messageId={}", message.getMessageId());
            errors.add(Pain002Message.ValidationError.builder()
                    .field("debtorAccount.iban")
                    .rejectionCode(Pain002Message.CODE_MISSING_DEBTOR_ACCOUNT)
                    .description("Debtor account number is missing or invalid")
                    .build());
        }

        // ── Creditor Account ────────────────────────────────────────────────
        String creditorIban = (txn != null && txn.getCreditorAccount() != null) ? txn.getCreditorAccount().getIban() : null;
        if (isBlank(creditorIban)) {
            log.warn("[VALIDATOR] FAIL - Creditor account (IBAN) is missing or blank | messageId={}", message.getMessageId());
            errors.add(Pain002Message.ValidationError.builder()
                    .field("creditorAccount.iban")
                    .rejectionCode(Pain002Message.CODE_MISSING_CREDITOR_ACCOUNT)
                    .description("Creditor account number is missing or invalid")
                    .build());
        }

        // ── Debtor Routing Number ───────────────────────────────────────────
        String debtorRouting = (pi != null) ? pi.getDebtorRoutingNumber() : null;
        if (isBlank(debtorRouting)) {
            log.warn("[VALIDATOR] FAIL - Debtor routing number (ABA) is missing | messageId={}", message.getMessageId());
            errors.add(Pain002Message.ValidationError.builder()
                    .field("debtorRoutingNumber")
                    .rejectionCode(Pain002Message.CODE_MISSING_ROUTING_NUMBER)
                    .description("Debtor ABA routing number is missing or invalid")
                    .build());
        } else if (!isValidAba(debtorRouting)) {
            log.warn("[VALIDATOR] FAIL - Debtor routing number is not a valid 9-digit ABA | value={} | messageId={}", debtorRouting, message.getMessageId());
            errors.add(Pain002Message.ValidationError.builder()
                    .field("debtorRoutingNumber")
                    .rejectionCode(Pain002Message.CODE_MISSING_ROUTING_NUMBER)
                    .description("Debtor ABA routing number must be a 9-digit number, got: " + debtorRouting)
                    .build());
        }

        // ── Creditor Routing Number ─────────────────────────────────────────
        String creditorRouting = (pi != null) ? pi.getCreditorRoutingNumber() : null;
        if (isBlank(creditorRouting)) {
            log.warn("[VALIDATOR] FAIL - Creditor routing number (ABA) is missing | messageId={}", message.getMessageId());
            errors.add(Pain002Message.ValidationError.builder()
                    .field("creditorRoutingNumber")
                    .rejectionCode(Pain002Message.CODE_MISSING_ROUTING_NUMBER)
                    .description("Creditor ABA routing number is missing or invalid")
                    .build());
        } else if (!isValidAba(creditorRouting)) {
            log.warn("[VALIDATOR] FAIL - Creditor routing number is not a valid 9-digit ABA | value={} | messageId={}", creditorRouting, message.getMessageId());
            errors.add(Pain002Message.ValidationError.builder()
                    .field("creditorRoutingNumber")
                    .rejectionCode(Pain002Message.CODE_MISSING_ROUTING_NUMBER)
                    .description("Creditor ABA routing number must be a 9-digit number, got: " + creditorRouting)
                    .build());
        }

        // ── Amount ──────────────────────────────────────────────────────────
        BigDecimal amount = (txn != null && txn.getInstructedAmount() != null) ? txn.getInstructedAmount().getValue() : null;
        if (amount == null) {
            log.warn("[VALIDATOR] FAIL - Amount is missing | messageId={}", message.getMessageId());
            errors.add(Pain002Message.ValidationError.builder()
                    .field("instructedAmount.value")
                    .rejectionCode(Pain002Message.CODE_MISSING_AMOUNT)
                    .description("Payment amount is missing")
                    .build());
        } else if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[VALIDATOR] FAIL - Amount must be greater than zero | amount={} | messageId={}", amount, message.getMessageId());
            errors.add(Pain002Message.ValidationError.builder()
                    .field("instructedAmount.value")
                    .rejectionCode(Pain002Message.CODE_INVALID_AMOUNT)
                    .description("Payment amount must be greater than zero, got: " + amount)
                    .build());
        }

        if (errors.isEmpty()) {
            log.info("[VALIDATOR] ✅ All mandatory fields valid | messageId={}", message.getMessageId());
            return ValidationResult.valid();
        } else {
            log.warn("[VALIDATOR] ❌ Validation failed with {} error(s) | messageId={}", errors.size(), message.getMessageId());
            return ValidationResult.invalid(errors);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isValidAba(String aba) {
        return aba != null && aba.matches("\\d{9}");
    }

    public record ValidationResult(boolean isValid, List<Pain002Message.ValidationError> errors) {
        public static ValidationResult valid() {
            return new ValidationResult(true, List.of());
        }
        public static ValidationResult invalid(List<Pain002Message.ValidationError> errors) {
            return new ValidationResult(false, errors);
        }
    }
}
