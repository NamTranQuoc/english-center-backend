package com.englishcenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class EnglishCenterApplication {
    public static void main(String[] args) {
        SpringApplication.run(EnglishCenterApplication.class, args);
    }

//    @Scheduled(fixedRate = 60000)
//    private void start0hEveryday() {
//        MemberApplication memberApplication = new MemberApplication();
//        memberApplication.test();
//    }
}
