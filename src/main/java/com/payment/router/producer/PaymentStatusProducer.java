package com.payment.router.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.router.model.Pain002Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PaymentStatusProducer {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${kafka.topic.payment-status}")
    private String statusTopic;

    public void publishRejection(Pain002Message pain002Message) {
        String paymentId = pain002Message.getOriginalPaymentId();
        try {
            String payload = objectMapper.writeValueAsString(pain002Message);

            log.info("[STATUS-PRODUCER] Publishing PAIN 002 rejection | paymentId={} | topic={} | errors={}",
                    paymentId, statusTopic, pain002Message.getValidationErrors().size());

            var result = kafkaTemplate.send(statusTopic, paymentId, payload).get(10, TimeUnit.SECONDS);

            log.info("[STATUS-PRODUCER] ✅ PAIN 002 published | paymentId={} | topic={} | partition={} | offset={}",
                    paymentId, statusTopic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (Exception e) {
            log.error("[STATUS-PRODUCER] ❌ Failed to publish PAIN 002 | paymentId={} | error={}", paymentId, e.getMessage(), e);
            throw new RuntimeException("Failed to publish PAIN 002 rejection message", e);
        }
    }
}
