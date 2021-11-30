package com.englishcenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableScheduling
public class EnglishCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnglishCenterApplication.class, args);
    }

    //start vào 00h00 mỗi ngày
    @Scheduled(cron = "0 0/1 0/1 ? * *")
    private void start0hEveryday() {

    }

    //start vào 07h00 mỗi ngày
    @Scheduled(cron = "0 0/1 7/1 ? * *")
    private void start7hEveryday() {

    }
}
