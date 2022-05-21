package com.englishcenter.exam.schedule.job;

import com.englishcenter.core.schedule.ScheduleName;
import com.englishcenter.core.schedule.TaskSchedulingService;
import com.englishcenter.exam.schedule.ExamSchedule;
import com.englishcenter.exam.schedule.application.ExamScheduleApplication;
import lombok.Data;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Data
public class ExamScheduleFinalJob implements Runnable {
    private String examScheduleId;
    private TaskSchedulingService taskSchedulingService;

    @SneakyThrows
    @Override
    public void run() {
        ExamScheduleApplication examScheduleApplication = new ExamScheduleApplication();

        taskSchedulingService.cleanJobWhenRun(ScheduleName.EXAM_SCHEDULE_FINAL, examScheduleId);

        //handle
        Optional<ExamSchedule> optional = examScheduleApplication.getById(examScheduleId);

        if (!optional.isPresent()) {
            return;
        }

        ExamSchedule examSchedule = optional.get();

        examSchedule.setStatus(ExamSchedule.ExamStatus.finish);
        examScheduleApplication.mongoDBConnection.update(examScheduleId, examSchedule);
    }
}
