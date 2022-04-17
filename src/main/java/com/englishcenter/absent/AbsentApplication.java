package com.englishcenter.absent;

import com.englishcenter.absent.command.*;
import com.englishcenter.classroom.ClassRoom;
import com.englishcenter.classroom.application.ClassRoomApplication;
import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.exam.schedule.ExamSchedule;
import com.englishcenter.member.Member;
import com.englishcenter.member.application.MemberApplication;
import com.englishcenter.schedule.Schedule;
import com.englishcenter.schedule.application.ScheduleApplication;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AbsentApplication {
    public final MongoDBConnection<Absent> mongoDBConnection;
    @Autowired
    private ScheduleApplication scheduleApplication;
    @Autowired
    private ClassRoomApplication classRoomApplication;
    @Autowired
    private MemberApplication memberApplication;
    @Autowired
    public AbsentApplication() {
        mongoDBConnection = new MongoDBConnection<>(MongodbEnum.collection_absent, Absent.class);
    }

    public Optional<CommandResponseGetStudent> getStudents(CommandGetStudent command) throws Exception {
        Map<String, Object> query = new HashMap<>();
        query.put("teacher_id", command.getCurrent_member_id());
        if (StringUtils.isNotBlank(command.getSchedule_id())) {
            query.put("_id", new ObjectId(command.getSchedule_id()));
        } else {
            long now = System.currentTimeMillis();
            query.put("start_date", new Document("$lte", now));
            query.put("end_date", new Document("$gte", now));
        }
        Optional<Schedule> optionalSchedule = scheduleApplication.mongoDBConnection.findOne(query);
        if (!optionalSchedule.isPresent()) {
            throw new Exception(ExceptionEnum.schedule_not_exist);
        }
        Schedule schedule = optionalSchedule.get();
        Optional<ClassRoom> optionalClassRoom = classRoomApplication.getById(schedule.getClassroom_id());
        if (!optionalClassRoom.isPresent()) {
            throw new Exception(ExceptionEnum.classroom_not_exist);
        }
        ClassRoom classRoom = optionalClassRoom.get();
        Map<String, Object> query1 = new HashMap<>();
        query1.put("backup_schedule_id", schedule.get_id().toHexString());
        List<Absent> absents = mongoDBConnection.find(query1).orElse(new ArrayList<>());
        List<ObjectId> ids = classRoom.getStudent_ids().stream().map(item -> new ObjectId(item.getStudent_id())).collect(Collectors.toList());
        ids.addAll(absents.stream().map(item -> new ObjectId(item.getStudent_id())).collect(Collectors.toList()));
        Map<String, Object> query2 = new HashMap<>();
        query2.put("schedule_id", schedule.get_id().toHexString());
        List<Absent> absents1 = mongoDBConnection.find(query2).orElse(new ArrayList<>());
        ids.removeAll(absents1.stream().map(item -> new ObjectId(item.getStudent_id())).collect(Collectors.toList()));
        Map<String, Object> queryStudent = new HashMap<>();
        queryStudent.put("_id", new Document("$in", ids));
        List<Member> students = memberApplication.mongoDBConnection.find(queryStudent).orElse(new ArrayList<>());
        return Optional.of(CommandResponseGetStudent.builder()
                .students(students)
                .schedule(schedule)
                .build());
    }

    public Optional<Schedule> saveAbsent(CommandMuster command) throws Exception {
        if (command.getStudent_ids() == null || StringUtils.isBlank(command.getSchedule_id())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        Optional<Schedule> optionalSchedule = scheduleApplication.mongoDBConnection.getById(command.getSchedule_id());
        if (!optionalSchedule.isPresent()) {
            throw new Exception(ExceptionEnum.schedule_not_exist);
        }
        Schedule schedule = optionalSchedule.get();
        long now = System.currentTimeMillis();
        if (!(schedule.getStart_date() - 900000 < now && schedule.getEnd_date() + 900000 > now)) {
            throw new Exception(ExceptionEnum.not_during_muster_time);
        }
        schedule.setAbsent_student_ids(command.getStudent_ids());
        return scheduleApplication.mongoDBConnection.update(schedule.get_id().toHexString(), schedule);
    }

    public Optional<List<CommandGetClassroomAbsent>> getClassroomAbsents(CommandGetAbsent command) throws Exception {
        if (StringUtils.isAnyBlank(command.getSchedule_id(), command.getStudent_id())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        Optional<Schedule> optionalSchedule = scheduleApplication.mongoDBConnection.getById(command.getSchedule_id());
        if (!optionalSchedule.isPresent()) {
            throw new Exception(ExceptionEnum.schedule_not_exist);
        }
        Schedule schedule = optionalSchedule.get();
        Map<String, Object> query = new HashMap<>();
        query.put("_id", new ObjectId(schedule.getClassroom_id()));
        query.put("student_ids.student_id", command.getStudent_id());
        Optional<ClassRoom> optionalClassRoom = classRoomApplication.mongoDBConnection.findOne(query);
        if (!optionalClassRoom.isPresent()) {
            throw new Exception(ExceptionEnum.cannot_register_absent);
        }
        ClassRoom classRoom = optionalClassRoom.get();
        long start = schedule.getStart_date();
        if (schedule.getSession() != 1) {
            Map<String, Object> query2 = new HashMap<>();
            query2.put("classroom_id", schedule.getClassroom_id());
            query2.put("session", schedule.getSession() - 1);
            Optional<Schedule> schedule1 = scheduleApplication.mongoDBConnection.findOne(query2);
            if (schedule1.isPresent()) {
                start = schedule1.get().getEnd_date();
            }
        }
        long end = schedule.getEnd_date() + 604800000L; //sau 7 ngày
        Map<String, Object> query3 = new HashMap<>();
        query3.put("classroom_id", schedule.getClassroom_id());
        query3.put("session", schedule.getSession() + 1);
        Optional<Schedule> schedule2 = scheduleApplication.mongoDBConnection.findOne(query3);
        if (schedule2.isPresent()) {
            end = schedule2.get().getStart_date();
        }
        Map<String, Object> query1 = new HashMap<>();
        query1.put("course_id", classRoom.getCourse_id());
        query1.put("status", ExamSchedule.ExamStatus.coming);
        List<ClassRoom> classRooms = classRoomApplication.find(query1).orElse(new ArrayList<>());
        Map<String, String> name = new HashMap<>();
        List<String> classroomIds = classRooms.stream().map(item -> {
            name.put(item.get_id().toHexString(), item.getName());
            return item.get_id().toHexString();
        }).collect(Collectors.toList());
        Map<String, Object> query4 = new HashMap<>();
        query4.put("classroom_id", new Document("$in", classroomIds));
        query4.put("start_date", new Document("$gt", start));
        query4.put("end_date", new Document("$lt", end));
        query4.put("session", schedule.getSession());
        List<Schedule> schedules = scheduleApplication.mongoDBConnection.find(query4).orElse(new ArrayList<>());
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy - hh:mm");
        return Optional.of(schedules.stream().map(item -> CommandGetClassroomAbsent.builder()
                ._id(item.getClassroom_id())
                .name(String.format("%s | %s", name.get(item.getClassroom_id()), formatter.format(new Date(item.getStart_date()))))
                .build()).collect(Collectors.toList()));
    }

    public Optional<Absent> registerAbsent(CommandRegisterAbsent command) throws Exception {
        if (StringUtils.isAnyBlank(command.getSchedule_id(), command.getStudent_id(), command.getClassroom_id())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        Optional<Schedule> optionalSchedule = scheduleApplication.mongoDBConnection.getById(command.getSchedule_id());
        if (!optionalSchedule.isPresent()) {
            throw new Exception(ExceptionEnum.schedule_not_exist);
        }
        Schedule schedule = optionalSchedule.get();
        Map<String, Object> query = new HashMap<>();
        query.put("schedule_id", schedule.get_id().toHexString());
        long count = mongoDBConnection.count(query).orElse(0L);
        if (count != 0) {
            throw new Exception(ExceptionEnum.absent_exist);
        }
        Map<String, Object> query1 = new HashMap<>();
        query1.put("classroom_id", command.getClassroom_id());
        query1.put("session", schedule.getSession());
        Optional<Schedule> schedule1 = scheduleApplication.mongoDBConnection.findOne(query1);
        if (!schedule1.isPresent()) {
            throw new Exception(ExceptionEnum.schedule_not_exist);
        }
        schedule.getAbsent_student_ids().add(command.getStudent_id());
        scheduleApplication.mongoDBConnection.update(schedule.get_id().toHexString(), schedule);
        Absent absent = Absent.builder()
                .student_id(command.getStudent_id())
                .backup_classroom_id(command.getClassroom_id())
                .session(schedule.getSession())
                .classroom_id(schedule.getClassroom_id())
                .schedule_id(schedule.get_id().toHexString())
                .backup_schedule_id(schedule1.get().get_id().toHexString())
                .build();
        return mongoDBConnection.insert(absent);
    }
}
