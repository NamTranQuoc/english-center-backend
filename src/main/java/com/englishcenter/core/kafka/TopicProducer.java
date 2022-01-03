package com.englishcenter.core.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicProducer {
    public static final String SEND_MAIL = "SEND_MAIL";
    public static final String UPDATE_SCORE_BY_EXCEL = "UPDATE_SCORE_BY_EXCEL";
    public static final String GENERATE_SCHEDULE = "GENERATE_SCHEDULE";

    @Bean
    public NewTopic sendMail() {
        return TopicBuilder.name(SEND_MAIL)
                .partitions(10)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic updateScore() {
        return TopicBuilder.name(UPDATE_SCORE_BY_EXCEL)
                .partitions(10)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic generateSchedule() {
        return TopicBuilder.name(GENERATE_SCHEDULE)
                .partitions(10)
                .replicas(1)
                .build();
    }
}
