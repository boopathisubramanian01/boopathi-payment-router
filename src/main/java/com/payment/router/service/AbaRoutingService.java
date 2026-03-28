package com.payment.router.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
public class AbaRoutingService {

    public static final String PAYMENT_TYPE_INSTANT = "Instant Payment";
    public static final String PAYMENT_TYPE_ACH     = "Ach Payment";

    /**
     * Dummy list of ABA routing numbers eligible for Instant Payment (RTP/FedNow).
     * In production this would be loaded from a database or external service.
     */
    private static final Set<String> INSTANT_PAYMENT_ABA_LIST = Set.of(
            "021000021", // JPMorgan Chase Bank
            "026009593", // Bank of America
            "021200339", // Citibank
            "011000138", // Bank of America (New England)
            "021101108", // Capital One
            "031176110", // TD Bank
            "322271627", // Wells Fargo (California)
            "121000358", // Wells Fargo (Western)
            "124303120", // US Bank
            "096010415"  // US Bank (Minnesota)
    );

    public RoutingDecision route(String abaRoutingNumber) {
        log.info("[ROUTER] Evaluating ABA routing number | aba={}", abaRoutingNumber);

        if (INSTANT_PAYMENT_ABA_LIST.contains(abaRoutingNumber)) {
            log.info("[ROUTER] ✅ ABA {} matched Instant Payment list → paymentType='{}'", abaRoutingNumber, PAYMENT_TYPE_INSTANT);
            return new RoutingDecision(
                    PAYMENT_TYPE_INSTANT,
                    "ABA " + abaRoutingNumber + " is registered for RTP/FedNow instant payment rails"
            );
        } else {
            log.info("[ROUTER] ABA {} not in Instant Payment list → paymentType='{}'", abaRoutingNumber, PAYMENT_TYPE_ACH);
            return new RoutingDecision(
                    PAYMENT_TYPE_ACH,
                    "ABA " + abaRoutingNumber + " is routed via ACH payment rails"
            );
        }
    }

    public record RoutingDecision(String paymentType, String reason) {}
}
