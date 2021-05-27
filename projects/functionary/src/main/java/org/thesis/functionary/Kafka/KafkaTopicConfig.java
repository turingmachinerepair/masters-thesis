package org.thesis.functionary.Kafka;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Класс конфигурации тем Kafka. При отсутствии темы в брокере он будет создан.
 * Аннотация @Configuration фреймворка Spring
 */
@Configuration
public class KafkaTopicConfig {

    /**
     * Адрес брокера Kafka
     */
    @Value("${kafka.broker.addr}")
    private String bootstrapAddress;

    /**
     * Сгенерировать экземпляр управляющего объекта Kafka из заданных параметров
     * @return экземпляр KafkaAdmin
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        return new KafkaAdmin(configs);
    }

    /**
     * Выдать спецификацию темы Kafka
     * @return экзмпляр спецификации темы Kafka для UUID задач
     */
    @Bean
    public NewTopic topic1() {
         return new NewTopic("TaskFabric", 1, (short) 1);
    }
}
