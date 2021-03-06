package com.englishcenter.exam.schedule.job;

import com.englishcenter.core.fcm.NotificationRequest;
import com.englishcenter.core.firebase.FirebaseFileService;
import com.englishcenter.core.mail.Mail;
import com.englishcenter.core.mail.MailService;
import com.englishcenter.core.schedule.ScheduleName;
import com.englishcenter.core.schedule.TaskSchedulingService;
import com.englishcenter.core.thymeleaf.ThymeleafService;
import com.englishcenter.exam.schedule.ExamSchedule;
import com.englishcenter.exam.schedule.application.ExamScheduleApplication;
import com.englishcenter.member.Member;
import com.englishcenter.member.application.MemberApplication;
import com.englishcenter.member.command.CommandSearchMember;
import com.englishcenter.room.Room;
import com.englishcenter.room.application.RoomApplication;
import lombok.Data;
import lombok.SneakyThrows;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Data
public class ExamScheduleRemindJob implements Runnable {
    private String examScheduleId;
    private TaskSchedulingService taskSchedulingService;
    private MailService mailService;
    private FirebaseFileService firebaseFileService;

    @SneakyThrows
    @Override
    public void run() {
        RoomApplication roomApplication = new RoomApplication();
        MemberApplication memberApplication = new MemberApplication();
        ThymeleafService thymeleafService = new ThymeleafService();
        ExamScheduleApplication examScheduleApplication = new ExamScheduleApplication();

        taskSchedulingService.cleanJobWhenRun(ScheduleName.EXAM_SCHEDULE_REMIND, examScheduleId);

        //handle
        Optional<ExamSchedule> optional = examScheduleApplication.getById(examScheduleId);

        if (!optional.isPresent()) {
            return;
        }

        ExamSchedule examSchedule = optional.get();

        Optional<Room> room = roomApplication.getById(examSchedule.getRoom_id());
        if (room.isPresent()) {
            Map<String, Object> data = new HashMap<>();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(examSchedule.getStart_time());
            calendar.setTimeZone(TimeZone.getTimeZone("GMT+07:00"));
            data.put("date", new SimpleDateFormat("dd/MM/yyyy").format(new Date(examSchedule.getStart_time())));
            data.put("room", room.get().getName());
            data.put("start_date", String.format("%02dh%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE)));

            List<ObjectId> ids = examSchedule.getStudent_ids().stream().map(ObjectId::new).collect(Collectors.toList());
            ids.addAll(examSchedule.getMember_ids().stream().map(ObjectId::new).collect(Collectors.toList()));
            List<Member> students = memberApplication.mongoDBConnection.find(new Document("_id", new Document("$in", ids)))
                    .orElse(new ArrayList<>());

            List<String> emails = students.stream().map(Member::getEmail).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(students) && examSchedule.getStudent_ids().size() >= examSchedule.getMin_quantity()) {
                mailService.send(Mail.builder()
                        .mail_tos(emails)
                        .mail_subject("Th??ng b??o!")
                        .mail_content(thymeleafService.getContent("mailRemindExam", data))
                        .build());

                Map<String, String> teachers = new HashMap<>();
                memberApplication.getAll(CommandSearchMember.builder()
                        .types(Arrays.asList(Member.MemberType.RECEPTIONIST, Member.MemberType.TEACHER))
                        .build()).orElse(new ArrayList<>()).forEach(item -> teachers.put(item.get_id(), item.getName()));
                StringBuilder a = new StringBuilder();
                for (String id : examSchedule.getMember_ids()) {
                    a.append(teachers.get(id)).append("\n");
                }

                Map<String, String> d = new HashMap<>();
                d.put("id", examSchedule.get_id().toHexString());
                d.put("type", "exam-schedule");
                d.put("title", examSchedule.getCode());
                d.put("teacher", String.valueOf(a));
                d.put("room", room.get().getName());
                d.put("start", examSchedule.getStart_time().toString());
                d.put("end", examSchedule.getEnd_time().toString());
                d.put("session", "0");
                d.put("max_student", "0");
                d.put("took_place", "false");
                d.put("classroom_id", "0");
                d.put("is_absent", "false");
                d.put("is_exam", "true");
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                formatter.setTimeZone(TimeZone.getTimeZone("GMT+07:00"));
                String title = "B???n c?? thi " + examSchedule.getCode() + " v??o " + formatter.format(new Date(examSchedule.getStart_time()));

                if (!CollectionUtils.isEmpty(students)) {
                    students.forEach(item -> {
                        if (!CollectionUtils.isEmpty(item.getTokens())) {
                            item.getTokens().forEach(sub -> {
                                firebaseFileService.sendPnsToDevice(NotificationRequest.builder()
                                        .target(sub)
                                        .title("Th??ng b??o l???ch thi")
                                        .body(title)
                                        .data(d)
                                        .build());
                            });
                        }
                    });
                }

                //update status exam schedule
                examSchedule.setStatus(ExamSchedule.ExamStatus.coming);
                examScheduleApplication.mongoDBConnection.update(examScheduleId, examSchedule);

                //job update status when exam finish
                ExamScheduleFinalJob examScheduleFinalJob = new ExamScheduleFinalJob();
                examScheduleFinalJob.setExamScheduleId(examScheduleId);
                examScheduleFinalJob.setTaskSchedulingService(taskSchedulingService);
                taskSchedulingService.scheduleATask(
                        examScheduleFinalJob,
                        examSchedule.getEnd_time(),
                        ScheduleName.EXAM_SCHEDULE_FINAL,
                        examScheduleId);
            } else {
                //update status exam schedule
                examSchedule.setStatus(ExamSchedule.ExamStatus.cancel);
                examScheduleApplication.mongoDBConnection.update(examScheduleId, examSchedule);

                if (!CollectionUtils.isEmpty(students)) {
                    Map<String, Object> data1 = new HashMap<>();
                    data1.put("reason", "L???ch thi c???a b???n v??o ng??y " + new SimpleDateFormat("dd/MM/yyyy - HH:mm").format(new Date(examSchedule.getStart_time())) + " ???? b??? h???y do kh??ng ????? s??? l?????ng ????ng k??");
                    List<String> emails1 = students
                            .stream().map(Member::getEmail).collect(Collectors.toList());

                    mailService.send(Mail.builder()
                            .mail_tos(emails1)
                            .mail_subject("Th??ng b??o!")
                            .mail_content(thymeleafService.getContent("mailWhenCancel", data1))
                            .build());

                    Map<String, String> d = new HashMap<>();
                    d.put("id", examSchedule.get_id().toHexString());
                    d.put("type", "cancel");
                    d.put("title", examSchedule.getCode());
                    d.put("teacher", "");
                    d.put("room", room.get().getName());
                    d.put("start", examSchedule.getStart_time().toString());
                    d.put("end", examSchedule.getEnd_time().toString());
                    d.put("session", "0");
                    d.put("max_student", "0");
                    d.put("took_place", "false");
                    d.put("classroom_id", "0");
                    d.put("is_absent", "false");
                    d.put("is_exam", "true");

                    students.forEach(item -> {
                        if (!CollectionUtils.isEmpty(item.getTokens())) {
                            item.getTokens().forEach(sub -> {
                                firebaseFileService.sendPnsToDevice(NotificationRequest.builder()
                                        .target(sub)
                                        .title("Th??ng b??o h???y l???ch thi")
                                        .body("L???ch thi ng??y " + new SimpleDateFormat("dd/MM/yyyy - HH:mm").format(new Date(examSchedule.getStart_time())) + " ???? b??? h???y")
                                        .data(d)
                                        .build());
                            });
                        }
                    });
                }
            }
        }
    }
}
