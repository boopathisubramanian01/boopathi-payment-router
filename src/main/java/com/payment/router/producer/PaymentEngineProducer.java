package com.payment.router.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.router.model.RoutedPaymentMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PaymentEngineProducer {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${kafka.topic.payment-engine}")
    private String engineTopic;

    public void publishToEngine(RoutedPaymentMessage routedMessage) {
        String paymentId = routedMessage.getOriginalPaymentId();
        String paymentType = routedMessage.getPaymentType();

        try {
            String payload = objectMapper.writeValueAsString(routedMessage);

            // Build ProducerRecord so we can attach message headers
            ProducerRecord<String, String> record = new ProducerRecord<>(engineTopic, paymentId, payload);

            // ── Add payment-type header ──────────────────────────────────────
            Headers headers = record.headers();
            headers.add("payment-type", paymentType.getBytes(StandardCharsets.UTF_8));
            headers.add("original-message-id", routedMessage.getOriginalMessageId().getBytes(StandardCharsets.UTF_8));
            headers.add("aba-routing-number", routedMessage.getAbaRoutingNumber().getBytes(StandardCharsets.UTF_8));

            log.info("[ENGINE-PRODUCER] Publishing routed payment | paymentId={} | paymentType={} | topic={}",
                    paymentId, paymentType, engineTopic);
            log.debug("[ENGINE-PRODUCER] Message headers: payment-type={} | aba={} | originalMessageId={}",
                    paymentType, routedMessage.getAbaRoutingNumber(), routedMessage.getOriginalMessageId());

            var result = kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);

            log.info("[ENGINE-PRODUCER] ✅ Routed payment published | paymentId={} | paymentType={} | partition={} | offset={}",
                    paymentId, paymentType,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (Exception e) {
            log.error("[ENGINE-PRODUCER] ❌ Failed to publish to engine | paymentId={} | error={}", paymentId, e.getMessage(), e);
            throw new RuntimeException("Failed to publish routed payment to engine", e);
        }
    }
}
