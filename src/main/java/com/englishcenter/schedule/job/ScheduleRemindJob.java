package com.englishcenter.schedule.job;

import com.englishcenter.absent.Absent;
import com.englishcenter.absent.AbsentApplication;
import com.englishcenter.classroom.ClassRoom;
import com.englishcenter.classroom.application.ClassRoomApplication;
import com.englishcenter.classroom.job.ClassroomFinalJob;
import com.englishcenter.core.fcm.NotificationRequest;
import com.englishcenter.core.firebase.FirebaseFileService;
import com.englishcenter.core.mail.Mail;
import com.englishcenter.core.mail.MailService;
import com.englishcenter.core.schedule.ScheduleName;
import com.englishcenter.core.schedule.TaskSchedulingService;
import com.englishcenter.core.thymeleaf.ThymeleafService;
import com.englishcenter.course.Course;
import com.englishcenter.course.application.CourseApplication;
import com.englishcenter.member.Member;
import com.englishcenter.member.application.MemberApplication;
import com.englishcenter.room.Room;
import com.englishcenter.room.application.RoomApplication;
import com.englishcenter.schedule.Schedule;
import com.englishcenter.schedule.application.ScheduleApplication;
import lombok.Data;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Data
public class ScheduleRemindJob implements Runnable {
    private String scheduleId;
    private TaskSchedulingService taskSchedulingService;
    private MailService mailService;
    private FirebaseFileService firebaseFileService;

    @Override
    public void run() {
        ClassRoomApplication classRoomApplication = new ClassRoomApplication();
        RoomApplication roomApplication = new RoomApplication();
        MemberApplication memberApplication = new MemberApplication();
        AbsentApplication absentApplication = new AbsentApplication();
        ThymeleafService thymeleafService = new ThymeleafService();
        ScheduleApplication scheduleApplication = new ScheduleApplication();
        CourseApplication courseApplication = new CourseApplication();

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
                formatter.setTimeZone(TimeZone.getTimeZone("GMT+07:00"));

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

                if (ClassRoom.Status.register.equals(classroom.get().getStatus())) {
                    if (classroom.get().getStudent_ids().size() >= classroom.get().getMin_student()) {
                        classroom.get().setStatus(ClassRoom.Status.coming);
                        classRoomApplication.mongoDBConnection.update(schedule.getClassroom_id(), classroom.get());
                    } else {
                        classroom.get().setStatus(ClassRoom.Status.cancel);
                        classRoomApplication.mongoDBConnection.update(schedule.getClassroom_id(), classroom.get());

                        Map<String, Object> data1 = new HashMap<>();
                        data1.put("reason", "Lớp học của bạn bắt đầu vào ngày " + formatter.format(new Date(classroom.get().getStart_date())) + " đã bị hủy do không đủ số lượng đăng ký");
                        mailService.send(Mail.builder()
                                .mail_tos(students)
                                .mail_subject("Thông báo!")
                                .mail_content(thymeleafService.getContent("mailWhenCancel", data1))
                                .build());

                        Map<String, String> d = new HashMap<>();
                        d.put("id", schedule.get_id().toHexString());
                        d.put("type", "cancel");
                        d.put("title", classroom.get().getName());
                        d.put("teacher", memberApplication.getById(schedule.getTeacher_id()).get().getName());
                        d.put("room", room.get().getName());
                        d.put("start", schedule.getStart_date().toString());
                        d.put("end", schedule.getEnd_date().toString());
                        d.put("session", schedule.getSession().toString());
                        d.put("course_id", classroom.get().getCourse_id());
                        d.put("max_student", classroom.get().getMax_student().toString());
                        d.put("took_place", "false");
                        d.put("classroom_id", classroom.get().get_id().toHexString());
                        d.put("is_absent", "true");
                        d.put("is_exam", "false");
                        members.forEach(item -> {
                            if (!CollectionUtils.isEmpty(item.getTokens())) {
                                item.getTokens().forEach(sub -> {
                                    firebaseFileService.sendPnsToDevice(NotificationRequest.builder()
                                            .target(sub)
                                            .title("Thông báo hủy lịch học")
                                            .body("Lớp học bắt đầu ngày " + new SimpleDateFormat("dd/MM/yyyy - HH:mm").format(new Date(classroom.get().getStart_date())) + " đã bị hủy")
                                            .data(d)
                                            .build());
                                });
                            }
                        });

                        Map<String, Object> querySchedule = new HashMap<>();
                        querySchedule.put("classroom_id", classroom.get().get_id().toHexString());
                        List<Schedule> list = scheduleApplication.mongoDBConnection.find(querySchedule).orElse(new ArrayList<>());
                        list.forEach(item -> {
                            taskSchedulingService.cleanJobWhenRun(ScheduleName.SCHEDULE_REMIND, item.get_id().toHexString());
                            scheduleApplication.mongoDBConnection.drop(item.get_id().toHexString());
                        });

                        return;
                    }
                }

                Optional<Course> course = courseApplication.mongoDBConnection.getById(classroom.get().getCourse_id());
                if (course.isPresent() && course.get().getNumber_of_shift().equals(schedule.getSession())) {
                    //job update status when classroom finish
                    ClassroomFinalJob classroomFinalJob = new ClassroomFinalJob();
                    classroomFinalJob.setClassroomId(classroom.get().get_id().toHexString());
                    classroomFinalJob.setTaskSchedulingService(taskSchedulingService);
                    taskSchedulingService.scheduleATask(
                            classroomFinalJob,
                            schedule.getEnd_date(),
                            ScheduleName.CLASSROOM_FINAL,
                            classroom.get().get_id().toHexString());
                }

                if (!CollectionUtils.isEmpty(students)) {
                    Mail mail = Mail.builder()
                            .mail_tos(students)
                            .mail_subject("Thông báo!")
                            .mail_content(thymeleafService.getContent("mailRemindSchedule", data))
                            .build();
                    mailService.send(mail);
                    Map<String, String> d = new HashMap<>();
                    d.put("id", schedule.get_id().toHexString());
                    d.put("type", "schedule");
                    d.put("title", classroom.get().getName());
                    d.put("teacher", memberApplication.getById(schedule.getTeacher_id()).get().getName());
                    d.put("room", room.get().getName());
                    d.put("start", schedule.getStart_date().toString());
                    d.put("end", schedule.getEnd_date().toString());
                    d.put("session", schedule.getSession().toString());
                    d.put("course_id", classroom.get().getCourse_id());
                    d.put("max_student", classroom.get().getMax_student().toString());
                    d.put("took_place", "false");
                    d.put("classroom_id", classroom.get().get_id().toHexString());
                    d.put("is_absent", "true");
                    d.put("is_exam", "false");
                    String title = "Bạn có lịch học lớp " + data.get("classroom") + " vào " + data.get("start_date");

                    if (!CollectionUtils.isEmpty(members)) {
                        members.forEach(item -> {
                            if (!CollectionUtils.isEmpty(item.getTokens())) {
                                d.put("is_absent", Boolean.toString(ids.contains(item.get_id())));
                                item.getTokens().forEach(sub -> {
                                    firebaseFileService.sendPnsToDevice(NotificationRequest.builder()
                                            .target(sub)
                                            .title("Thông báo lịch Học")
                                            .body(title)
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
}
