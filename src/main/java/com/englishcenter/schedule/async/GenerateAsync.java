package com.englishcenter.schedule.async;

import com.englishcenter.classroom.ClassRoom;
import com.englishcenter.classroom.application.ClassRoomApplication;
import com.englishcenter.core.schedule.ScheduleName;
import com.englishcenter.core.schedule.TaskSchedulingService;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.course.Course;
import com.englishcenter.course.application.CourseApplication;
import com.englishcenter.log.Log;
import com.englishcenter.log.LogApplication;
import com.englishcenter.member.Member;
import com.englishcenter.schedule.Schedule;
import com.englishcenter.schedule.application.ScheduleApplication;
import com.englishcenter.schedule.command.CommandAddSchedule;
import com.englishcenter.schedule.job.ScheduleRemindJob;
import com.englishcenter.shift.Shift;
import com.englishcenter.shift.application.ShiftApplication;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class GenerateAsync {
    @Autowired
    private ClassRoomApplication classRoomApplication;
    @Autowired
    private CourseApplication courseApplication;
    @Autowired
    private ShiftApplication shiftApplication;
    @Autowired
    private ScheduleApplication scheduleApplication;
    @Autowired
    private LogApplication logApplication;
    @Autowired
    private TaskSchedulingService taskSchedulingService;
    private final long TEN_MINUTE = 600000;

    @Async
    public void generateEven(CommandAddSchedule command) throws Exception {
        if (StringUtils.isAnyBlank(command.getClassroom_id(), command.getRole())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        Optional<ClassRoom> classRoomOptional = classRoomApplication.getById(command.getClassroom_id());
        if (!classRoomOptional.isPresent()) {
            throw new Exception(ExceptionEnum.classroom_not_exist);
        }
        ClassRoom classRoom = classRoomOptional.get();
        if (!ClassRoom.Status.create.equals(classRoom.getStatus())) {
            throw new Exception(ExceptionEnum.can_only_generate_with_status_is_create);
        }
        Optional<Course> optionalCourse = courseApplication.getById(classRoom.getCourse_id());
        if (!optionalCourse.isPresent()) {
            throw new Exception(ExceptionEnum.course_not_exist);
        }
        Optional<Shift> optionalShift = shiftApplication.getById(classRoom.getShift_id());
        if (!optionalShift.isPresent()) {
            throw new Exception(ExceptionEnum.shift_not_exist);
        }
        Shift shift = optionalShift.get();
        Course course = optionalCourse.get();
        scheduleApplication.validateScheduleExits(classRoom.get_id().toHexString());
        List<String> listTeacher = scheduleApplication.getTeacherIds(command.getTeacher_id(), course.get_id().toHexString());
        List<String> listRoom = scheduleApplication.getRoomIds(command.getRoom_id(), classRoom.getMax_student());
        List<Schedule> schedule = new ArrayList<>();
        if (CollectionUtils.isEmpty(listRoom)) {
            throw new Exception(ExceptionEnum.room_not_exist);
        }
        if (CollectionUtils.isEmpty((listTeacher))) {
            throw new Exception(ExceptionEnum.member_not_exist);
        }

        Date time = new Timestamp(classRoom.getStart_date());
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = dateFormat.format(time);
        long start = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(strDate + " " + shift.getFrom()).getTime();
        long end = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(strDate + " " + shift.getTo()).getTime();
        Calendar cStart = GregorianCalendar.getInstance();
        cStart.setTimeInMillis(start);
        Calendar cEnd = GregorianCalendar.getInstance();
        cEnd.setTimeInMillis(end);
        int i = 1;
        while (i <= course.getNumber_of_shift()) {
            int dayOfWeek = cStart.get(Calendar.DAY_OF_WEEK);
            if (classRoom.getDow().contains(dayOfWeek)) {
                String roomId = null;
                Map<String, Object> query = new HashMap<>();
                query.put("$or", Arrays.asList(
                        new Document("$and", Arrays.asList(
                                new Document("start_date", new Document("$lte", cStart.getTimeInMillis())),
                                new Document("end_date", new Document("$gte", cStart.getTimeInMillis()))
                        )),
                        new Document("$and", Arrays.asList(
                                new Document("start_date", new Document("$lte", cEnd.getTimeInMillis())),
                                new Document("end_date", new Document("$gte", cEnd.getTimeInMillis()))
                        ))
                ));
                for (String item : listRoom) {
                    query.put("room_id", item);
                    long count = scheduleApplication.mongoDBConnection.count(query).orElse(0L);
                    if (count == 0) {
                        roomId = item;
                        break;
                    }
                }
                String teacherId = null;
                if (roomId != null) {
                    query.remove("room_id");
                    for (String item : listTeacher) {
                        query.put("teacher_id", item);
                        long count = scheduleApplication.mongoDBConnection.count(query).orElse(0L);
                        if (count == 0) {
                            teacherId = item;
                            break;
                        }
                    }
                }
                if (teacherId != null) {
                    Schedule newSchedule = Schedule.builder()
                            .room_id(roomId)
                            .teacher_id(teacherId)
                            .classroom_id(classRoom.get_id().toHexString())
                            .start_date(cStart.getTimeInMillis())
                            .end_date(cEnd.getTimeInMillis())
                            .session(i)
                            .build();
                    schedule.add(newSchedule);
                    i++;
                }
            }
            cStart.add(Calendar.DATE, 1);
            cEnd.add(Calendar.DATE, 1);
        }
        classRoom.setStatus("register");
        classRoomApplication.mongoDBConnection.update(classRoom.get_id().toHexString(), classRoom);
        logApplication.mongoDBConnection.insert(Log.builder()
                .class_name(MongodbEnum.collection_schedule)
                .action(Log.ACTION.generate)
                .perform_by(command.getCurrent_member_id())
                .name(classRoom.getName())
                .build());
        boolean result = scheduleApplication.mongoDBConnection.insertMany(schedule).orElse(false);
        if (result) {
            Map<String, Object> queryGetListSchedule = new HashMap<>();
            queryGetListSchedule.put("classroom_id", command.getClassroom_id());
            List<Schedule> schedules = scheduleApplication.mongoDBConnection.find(queryGetListSchedule).orElse(new ArrayList<>());
            if (!CollectionUtils.isEmpty(schedules)) {
                for (Schedule schedule1 : schedules) {
                    ScheduleRemindJob scheduleRemindJob = new ScheduleRemindJob();
                    scheduleRemindJob.setScheduleId(schedule1.get_id().toHexString());
                    scheduleRemindJob.setTaskSchedulingService(taskSchedulingService);
                    taskSchedulingService.scheduleATask(
                            scheduleRemindJob,
                            schedule1.getStart_date() - TEN_MINUTE,
                            ScheduleName.SCHEDULE_REMIND,
                            schedule1.get_id().toHexString());
                }
            }
        }
    }
}
