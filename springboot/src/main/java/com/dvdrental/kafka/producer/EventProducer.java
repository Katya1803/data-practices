package com.dvdrental.kafka.producer;

import com.dvdrental.config.KafkaConfig;
import com.dvdrental.kafka.event.PaymentProcessedEvent;
import com.dvdrental.kafka.event.RentalCreatedEvent;
import com.dvdrental.kafka.event.RentalReturnedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendRentalCreated(RentalCreatedEvent event) {
        kafkaTemplate.send(KafkaConfig.TOPIC_RENTAL_CREATED, String.valueOf(event.getRentalId()), event);
        log.info("Sent rental.created rentalId={}", event.getRentalId());
    }

    public void sendRentalReturned(RentalReturnedEvent event) {
        kafkaTemplate.send(KafkaConfig.TOPIC_RENTAL_RETURNED, String.valueOf(event.getRentalId()), event);
        log.info("Sent rental.returned rentalId={}", event.getRentalId());
    }

    public void sendPaymentProcessed(PaymentProcessedEvent event) {
        kafkaTemplate.send(KafkaConfig.TOPIC_PAYMENT_PROCESSED, String.valueOf(event.getPaymentId()), event);
        log.info("Sent payment.processed paymentId={}", event.getPaymentId());
    }
}
