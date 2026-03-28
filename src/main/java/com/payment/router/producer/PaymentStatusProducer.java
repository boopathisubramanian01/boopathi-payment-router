package com.payment.router.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.router.model.Pain002Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
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
        String routerId = "RTR-" + UUID.randomUUID().toString().toUpperCase();
        try {
            String payload = objectMapper.writeValueAsString(pain002Message);

            ProducerRecord<String, String> record = new ProducerRecord<>(statusTopic, paymentId, payload);
            record.headers().add("router-message-id", routerId.getBytes(StandardCharsets.UTF_8));
            record.headers().add("original-message-id", pain002Message.getOriginalMessageId().getBytes(StandardCharsets.UTF_8));
            record.headers().add("transaction-status", Pain002Message.STATUS_RJCT.getBytes(StandardCharsets.UTF_8));

            log.info("[STATUS-PRODUCER] Publishing PAIN 002 rejection | paymentId={} | routerId={} | topic={} | errors={}",
                    paymentId, routerId, statusTopic, pain002Message.getValidationErrors().size());

            var result = kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);

            log.info("[STATUS-PRODUCER] ✅ PAIN 002 published | paymentId={} | routerId={} | partition={} | offset={}",
                    paymentId, routerId,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (Exception e) {
            log.error("[STATUS-PRODUCER] ❌ Failed to publish PAIN 002 | paymentId={} | error={}", paymentId, e.getMessage(), e);
            throw new RuntimeException("Failed to publish PAIN 002 rejection message", e);
        }
    }
}
