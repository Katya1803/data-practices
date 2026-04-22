package com.dvdrental.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.Map;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_RENTAL_CREATED    = "rental.created";
    public static final String TOPIC_RENTAL_RETURNED   = "rental.returned";
    public static final String TOPIC_PAYMENT_PROCESSED = "payment.processed";

    public static final String STATS_GROUP = "springboot-stats-group";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

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

    /**
     * Dedicated listener factory for the stats consumer.
     * Uses StringDeserializer so each handler receives raw JSON
     * and parses it independently — avoids type-header dependency
     * since the producer sets spring.json.add.type.headers=false.
     */
    @Bean("statsListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> statsListenerContainerFactory() {
        var props = Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,  bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG,           STATS_GROUP,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,  "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        );
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props));
        return factory;
    }
}
