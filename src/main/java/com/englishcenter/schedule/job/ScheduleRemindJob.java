package com.englishcenter.schedule.job;

import com.englishcenter.absent.Absent;
import com.englishcenter.absent.AbsentApplication;
import com.englishcenter.classroom.ClassRoom;
import com.englishcenter.classroom.application.ClassRoomApplication;
import com.englishcenter.core.fcm.NotificationRequest;
import com.englishcenter.core.firebase.FirebaseFileService;
import com.englishcenter.core.kafka.TopicProducer;
import com.englishcenter.core.mail.Mail;
import com.englishcenter.core.mail.MailService;
import com.englishcenter.core.schedule.ScheduleName;
import com.englishcenter.core.schedule.TaskSchedulingService;
import com.englishcenter.core.thymeleaf.ThymeleafService;
import com.englishcenter.member.Member;
import com.englishcenter.member.application.MemberApplication;
import com.englishcenter.room.Room;
import com.englishcenter.room.application.RoomApplication;
import com.englishcenter.schedule.Schedule;
import com.englishcenter.schedule.application.ScheduleApplication;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Data
public class ScheduleRemindJob implements Runnable {
    private String scheduleId;
    private KafkaTemplate<String, Mail> mailKafkaTemplate;

    @Override
    public void run() {
        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();
        ClassRoomApplication classRoomApplication = new ClassRoomApplication();
        RoomApplication roomApplication = new RoomApplication();
        MemberApplication memberApplication = new MemberApplication();
        AbsentApplication absentApplication = new AbsentApplication();
        ThymeleafService thymeleafService = new ThymeleafService();
        ScheduleApplication scheduleApplication = new ScheduleApplication();
        FirebaseFileService firebaseFileService = new FirebaseFileService();

        taskSchedulingService.cleanJobWhenRun(ScheduleName.SCHEDULE_REMIND, scheduleId);

        //handle
        Optional<Schedule> optional = scheduleApplication.mongoDBConnection.getById(scheduleId);
        if (optional.isPresent()) {
            Schedule schedule = optional.get();
            Optional<Room> room = roomApplication.getById(schedule.getRoom_id());
            Optional<ClassRoom> classroom = classRoomApplication.mongoDBConnection.getById(schedule.getClassroom_id());
            if (room.isPresent() && classroom.isPresent()) {
                Map<String, Object> data = new HashMap<>();
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");

                data.put("classroom", classroom.get().getName());
                data.put("room", room.get().getName());
                data.put("start_date", formatter.format(new Date(schedule.getStart_date())));

                Map<String, Object> query = new HashMap<>();
                query.put("schedule_id", schedule.get_id().toHexString());
                List<Absent> absents = absentApplication.mongoDBConnection.find(query).orElse(new ArrayList<>());
                List<String> absentsIds = absents.stream().map(Absent::getStudent_id).collect(Collectors.toList());

                List<ObjectId> ids = classroom.get().getStudent_ids().stream()
                        .filter(item -> !schedule.getAbsent_student_ids().contains(item.getStudent_id()) && !absentsIds.contains(item.getStudent_id()))
                        .map(item -> new ObjectId(item.getStudent_id())).collect(Collectors.toList());
                List<Member> members = memberApplication.mongoDBConnection.find(new Document("_id", new Document("$in", ids)))
                        .orElse(new ArrayList<>());
                List<String> students = members
                        .stream().map(Member::getEmail).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(students)) {
                    Mail mail = Mail.builder()
                            .mail_tos(students)
                            .mail_subject("Thông báo!")
                            .mail_content(thymeleafService.getContent("mailRemindSchedule", data))
                            .build();
                    mailKafkaTemplate.send(TopicProducer.SEND_MAIL, mail);
                    Map<String, String> d = new HashMap<>();
                    d.put("schedule_id", schedule.get_id().toHexString());
                    d.put("type", "schedule");
                    String title = "Bạn có lịch học lớp " + data.get("classroom") + " vào " + data.get("start_date");

                    members.forEach(item -> {
                        if (StringUtils.isNotBlank(item.getToken())) {
                            firebaseFileService.sendPnsToDevice(NotificationRequest.builder()
                                    .target(item.getToken())
                                    .title("Thông báo lịch Học")
                                    .body(title)
                                    .data(d)
                                    .build());
                        }
                    });
                }
            }
        }
    }
}
