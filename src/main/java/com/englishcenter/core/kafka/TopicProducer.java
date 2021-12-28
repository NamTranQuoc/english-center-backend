package com.englishcenter.core.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicProducer {
    public static final String SEND_MAIL = "SEND_MAIL";

    @Bean
    public NewTopic sendMail() {
        return TopicBuilder.name(SEND_MAIL)
                .partitions(10)
                .replicas(1)
                .build();
    }
}
