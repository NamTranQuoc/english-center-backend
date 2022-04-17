package com.englishcenter;

import com.englishcenter.classroom.application.ClassRoomApplication;
import com.englishcenter.exam.schedule.application.ExamScheduleApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableScheduling
public class EnglishCenterApplication {
    @Autowired
    private ExamScheduleApplication examScheduleApplication;
    @Autowired
    private ClassRoomApplication classRoomApplication;

    public static void main(String[] args) {
        SpringApplication.run(EnglishCenterApplication.class, args);
    }

    //start vào 00h00 mỗi ngày
    @Scheduled(cron = "0 0 0 ? * *")
    private void start0hEveryday() {
        //thông báo trước khi thi
        examScheduleApplication.sendMailRemind();
        examScheduleApplication.updateStatusExam();
//        classRoomApplication.sendMailRemind();
        classRoomApplication.updateStatusExam();
    }

    //start vào 07h00 mỗi ngày
    @Scheduled(cron = "0 0 7 ? * *")
    private void start7hEveryday() {

    }
}
