package org.thesis.functionary.Kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Класс-издатель для брокера Kafka
 */
@Configuration
public class KafkaProducerConfig {

    /**
     * Адрес брокера Kafka
     */

    @Value("${kafka.broker.addr}")
    String bootstrapAddress;

    /**
     * Сгенерировать экземпляр ProducerFactory для брокера Kafka.
     * @return фабрику издателей Kafka сконфигурированный для передачи UUID
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(
          ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
          bootstrapAddress);
        configProps.put(
          ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, 
          StringSerializer.class);
        configProps.put(
          ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
          StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Сгенерировать экземпляр издателя UUID в тему TaskFabric
     * @return экземпляр издателя UUID в тему TaskFabric
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {

        return new KafkaTemplate<>(producerFactory());
    }
}
