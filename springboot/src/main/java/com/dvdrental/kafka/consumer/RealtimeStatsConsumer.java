package com.dvdrental.kafka.consumer;

import com.dvdrental.config.KafkaConfig;
import com.dvdrental.service.RealtimeStatsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeStatsConsumer {

    private final RealtimeStatsService statsService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = KafkaConfig.TOPIC_RENTAL_CREATED,
            containerFactory = "statsListenerContainerFactory"
    )
    public void onRentalCreated(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            log.info("Stats consumer — rental.created rentalId={}", node.path("rentalId").asInt());
            statsService.incrementRentals(LocalDate.now());
        } catch (Exception e) {
            log.error("Stats consumer — failed to process rental.created: {}", e.getMessage());
        }
    }

    @KafkaListener(
            topics = KafkaConfig.TOPIC_RENTAL_RETURNED,
            containerFactory = "statsListenerContainerFactory"
    )
    public void onRentalReturned(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            log.info("Stats consumer — rental.returned rentalId={}", node.path("rentalId").asInt());
            statsService.incrementReturns(LocalDate.now());
        } catch (Exception e) {
            log.error("Stats consumer — failed to process rental.returned: {}", e.getMessage());
        }
    }

    @KafkaListener(
            topics = KafkaConfig.TOPIC_PAYMENT_PROCESSED,
            containerFactory = "statsListenerContainerFactory"
    )
    public void onPaymentProcessed(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            BigDecimal amount = new BigDecimal(node.path("amount").asText());
            log.info("Stats consumer — payment.processed paymentId={} amount={}",
                    node.path("paymentId").asInt(), amount);
            statsService.recordPayment(LocalDate.now(), amount);
        } catch (Exception e) {
            log.error("Stats consumer — failed to process payment.processed: {}", e.getMessage());
        }
    }
}
