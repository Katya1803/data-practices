package com.dvdrental.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_RENTAL_CREATED   = "rental.created";
    public static final String TOPIC_RENTAL_RETURNED  = "rental.returned";
    public static final String TOPIC_PAYMENT_PROCESSED = "payment.processed";

    @Bean
    public NewTopic rentalCreatedTopic() {
        return TopicBuilder.name(TOPIC_RENTAL_CREATED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic rentalReturnedTopic() {
        return TopicBuilder.name(TOPIC_RENTAL_RETURNED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic paymentProcessedTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_PROCESSED).partitions(1).replicas(1).build();
    }
}
