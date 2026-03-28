package com.payment.router.service;

import com.payment.router.model.Pain001Message;
import com.payment.router.model.Pain002Message;
import com.payment.router.model.RoutedPaymentMessage;
import com.payment.router.producer.PaymentEngineProducer;
import com.payment.router.producer.PaymentStatusProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class PaymentRouterService {

    @Autowired
    private PaymentValidationService validationService;

    @Autowired
    private AbaRoutingService abaRoutingService;

    @Autowired
    private PaymentStatusProducer statusProducer;

    @Autowired
    private PaymentEngineProducer engineProducer;

    public void route(Pain001Message message) {
        String messageId = message.getMessageId();
        String paymentId = extractPaymentId(message);

        log.info("[ROUTER] ══════════════════════════════════════════════════");
        log.info("[ROUTER] Processing incoming PAIN 001 message | messageId={} | paymentId={}", messageId, paymentId);

        // ── Step 1: Mandatory field validation ───────────────────────────────
        log.info("[ROUTER] STEP 1 - Mandatory field validation | messageId={}", messageId);
        PaymentValidationService.ValidationResult validation = validationService.validate(message);

        if (!validation.isValid()) {
            log.warn("[ROUTER] STEP 1 - Validation FAILED | messageId={} | errorCount={}", messageId, validation.errors().size());
            publishRejection(message, paymentId, validation);
            return;
        }
        log.info("[ROUTER] STEP 1 - Validation passed ✅ | messageId={}", messageId);

        // ── Step 2: ABA routing decision ─────────────────────────────────────
        log.info("[ROUTER] STEP 2 - Determining payment route via ABA | messageId={}", messageId);
        String abaRoutingNumber = message.getPaymentInformation().getDebtorRoutingNumber();
        AbaRoutingService.RoutingDecision decision = abaRoutingService.route(abaRoutingNumber);
        log.info("[ROUTER] STEP 2 - Route decided | messageId={} | paymentType={} | reason={}",
                messageId, decision.paymentType(), decision.reason());

        // ── Step 3: Build routed message with payment type ───────────────────
        log.info("[ROUTER] STEP 3 - Building routed payment message | paymentType={}", decision.paymentType());
        RoutedPaymentMessage routedMessage = RoutedPaymentMessage.builder()
                .originalMessageId(messageId)
                .originalPaymentId(paymentId)
                .paymentType(decision.paymentType())
                .abaRoutingNumber(abaRoutingNumber)
                .routingDecisionReason(decision.reason())
                .originalMessage(message)
                .build();

        // ── Step 4: Publish to payment engine topic with header ───────────────
        log.info("[ROUTER] STEP 4 - Publishing to payment engine | paymentId={} | paymentType={}", paymentId, decision.paymentType());
        engineProducer.publishToEngine(routedMessage);

        log.info("[ROUTER] ✅ Payment routed successfully | paymentId={} | paymentType={}", paymentId, decision.paymentType());
        log.info("[ROUTER] ══════════════════════════════════════════════════");
    }

    private void publishRejection(Pain001Message message, String paymentId,
                                   PaymentValidationService.ValidationResult validation) {
        String originalDebtorIban = null;
        String originalCreditorIban = null;
        String originalAmount = null;

        try {
            originalDebtorIban = message.getPaymentInformation().getDebtorAccount().getIban();
        } catch (Exception ignored) {}
        try {
            originalCreditorIban = message.getPaymentInformation()
                    .getCreditTransferTransaction().getCreditorAccount().getIban();
        } catch (Exception ignored) {}
        try {
            originalAmount = message.getPaymentInformation()
                    .getCreditTransferTransaction().getInstructedAmount().getValue().toPlainString();
        } catch (Exception ignored) {}

        Pain002Message pain002 = Pain002Message.builder()
                .messageId(UUID.randomUUID().toString())
                .creationDateTime(LocalDateTime.now())
                .originalMessageId(message.getMessageId())
                .originalPaymentId(paymentId)
                .transactionStatus(Pain002Message.STATUS_REJECTED)
                .statusReasonCode(validation.errors().get(0).getRejectionCode())
                .statusReasonDescription(validation.errors().get(0).getDescription())
                .validationErrors(validation.errors())
                .rejectedAt(LocalDateTime.now())
                .originalDebtorAccount(originalDebtorIban)
                .originalCreditorAccount(originalCreditorIban)
                .originalAmount(originalAmount)
                .build();

        log.warn("[ROUTER] Publishing PAIN 002 rejection | paymentId={} | reasons={}", paymentId,
                validation.errors().stream().map(Pain002Message.ValidationError::getRejectionCode).toList());

        statusProducer.publishRejection(pain002);

        log.warn("[ROUTER] ❌ Payment rejected and PAIN 002 sent | paymentId={}", paymentId);
        log.info("[ROUTER] ══════════════════════════════════════════════════");
    }

    private String extractPaymentId(Pain001Message message) {
        try {
            return message.getPaymentInformation()
                    .getCreditTransferTransaction()
                    .getPaymentId();
        } catch (Exception e) {
            return message.getMessageId();
        }
    }
}
