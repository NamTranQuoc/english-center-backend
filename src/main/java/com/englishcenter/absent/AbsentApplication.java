package com.englishcenter.absent;

import com.englishcenter.absent.command.CommandGetStudent;
import com.englishcenter.absent.command.CommandMuster;
import com.englishcenter.absent.command.CommandResponseGetStudent;
import com.englishcenter.classroom.ClassRoom;
import com.englishcenter.classroom.application.ClassRoomApplication;
import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.member.Member;
import com.englishcenter.member.application.MemberApplication;
import com.englishcenter.schedule.Schedule;
import com.englishcenter.schedule.application.ScheduleApplication;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class AbsentApplication {
    public final MongoDBConnection<Absent> mongoDBConnection;

    @Autowired
    public AbsentApplication() {
        mongoDBConnection = new MongoDBConnection<>(MongodbEnum.collection_absent, Absent.class);
    }

    @Autowired
    private ScheduleApplication scheduleApplication;
    @Autowired
    private ClassRoomApplication classRoomApplication;
    @Autowired
    private MemberApplication memberApplication;

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
        List<ObjectId> ids = classRoom.getStudent_ids().stream().map(item -> new ObjectId(item.getStudent_id())).collect(Collectors.toList());
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
}
