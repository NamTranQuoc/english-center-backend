package com.englishcenter.core.schedule;

import com.englishcenter.core.mail.Mail;
import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.schedule.job.ScheduleRemindJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

@Service
public class TaskSchedulingService {
    public final MongoDBConnection<Job> mongoDBConnection;
    Map<String, ScheduledFuture<?>> jobsMap = new HashMap<>();
    @Autowired
    private TaskScheduler taskScheduler;
    @Autowired
    private KafkaTemplate<String, Mail> mailKafkaTemplate;

    @Autowired
    public TaskSchedulingService() {
        mongoDBConnection = new MongoDBConnection<>(MongodbEnum.collection_job, Job.class);
    }

    public void scheduleATask(Runnable tasklet, long startTime, String name, String refId) {
        Optional<Job> optional = mongoDBConnection.insert(Job.builder()
                .name(name)
                .ref_id(refId)
                .start_time(startTime)
                .build());
        if (optional.isPresent()) {
            ScheduledFuture<?> scheduledTask = taskScheduler.schedule(tasklet, Instant.ofEpochMilli(startTime));
            jobsMap.put(optional.get().get_id().toHexString(), scheduledTask);
        }
    }

    public void removeScheduledTask(String name, String refId) {
        Optional<Job> optional = findByNameAndRefId(name, refId);
        if (optional.isPresent()) {
            String id = optional.get().get_id().toHexString();
            ScheduledFuture<?> scheduledTask = jobsMap.get(id);
            if (scheduledTask != null) {
                scheduledTask.cancel(true);
                jobsMap.remove(id);
            }
            mongoDBConnection.drop(id);
        }
    }

    public Optional<Job> findByNameAndRefId(String name, String refId) {
        Map<String, Object> query = new HashMap<>();
        query.put("name", name);
        query.put("ref_id", refId);
        return mongoDBConnection.findOne(query);
    }

    public void cleanJobWhenRun(String name, String refId) {
        Optional<Job> job = findByNameAndRefId(name, refId);
        if (job.isPresent()) {
            String id = job.get().get_id().toHexString();
            jobsMap.remove(id);
            mongoDBConnection.drop(id);
        }
    }

    public void removeScheduledTask(String id) {
        ScheduledFuture<?> scheduledTask = jobsMap.get(id);
        if (null != scheduledTask) {
            scheduledTask.cancel(true);
            jobsMap.remove(id);
            mongoDBConnection.drop(id);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStart() {
        List<Job> jobs = mongoDBConnection.find(new HashMap<>()).orElse(new ArrayList<>());
        for (Job job : jobs) {
            Runnable runnable = getTask(job);
            assert runnable != null;
            ScheduledFuture<?> scheduledTask = taskScheduler.schedule(runnable, Instant.ofEpochMilli(job.getStart_time()));
            jobsMap.put(job.get_id().toHexString(), scheduledTask);
        }
    }

    private Runnable getTask(Job job) {
        switch (job.getName()) {
            case ScheduleName.SCHEDULE_REMIND:
                ScheduleRemindJob scheduleRemindJob = new ScheduleRemindJob();
                scheduleRemindJob.setScheduleId(job.getRef_id());
                scheduleRemindJob.setMailKafkaTemplate(mailKafkaTemplate);
                return scheduleRemindJob;
        }
        return null;
    }
}
