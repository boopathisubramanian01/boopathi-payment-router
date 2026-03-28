package com.payment.router.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.router.model.Pain001Message;
import com.payment.router.service.PaymentRouterService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Pain001Consumer {

    @Autowired
    private PaymentRouterService routerService;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topic.pain001}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {
        String key = record.key();
        String value = record.value();
        long offset = record.offset();
        int partition = record.partition();

        log.info("[CONSUMER] ── Received PAIN 001 message ──────────────────────────");
        log.info("[CONSUMER] topic={} | partition={} | offset={} | key={}",
                record.topic(), partition, offset, key);
        log.debug("[CONSUMER] payload size={} bytes", value != null ? value.length() : 0);

        try {
            Pain001Message pain001 = objectMapper.readValue(value, Pain001Message.class);
            log.info("[CONSUMER] Deserialized PAIN 001 | messageId={} | version={}",
                    pain001.getMessageId(), pain001.getVersion());

            routerService.route(pain001);

        } catch (Exception e) {
            log.error("[CONSUMER] ❌ Failed to process message | key={} | partition={} | offset={} | error={}",
                    key, partition, offset, e.getMessage(), e);
        }
    }
}
