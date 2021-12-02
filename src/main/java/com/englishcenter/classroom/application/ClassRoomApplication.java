package com.englishcenter.classroom.application;

import com.englishcenter.classroom.ClassRoom;
import com.englishcenter.classroom.command.CommandAddClassRoom;
import com.englishcenter.classroom.command.CommandSearchClassRoom;
import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.Paging;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.log.Log;
import com.englishcenter.log.LogApplication;
import com.englishcenter.member.Member;
import com.englishcenter.schedule.application.ScheduleApplication;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class ClassRoomApplication {
    public final MongoDBConnection<ClassRoom> mongoDBConnection;

    @Autowired
    public ClassRoomApplication() {
        mongoDBConnection = new MongoDBConnection<>(MongodbEnum.collection_class_room, ClassRoom.class);
    }
    @Autowired
    private ScheduleApplication scheduleApplication;
    @Autowired
    private LogApplication logApplication;

    public Optional<ClassRoom> add(CommandAddClassRoom command) throws Exception {
        if (StringUtils.isAnyBlank(command.getName(), command.getCourse_id(), command.getShift_id())
                || command.getMax_student() == null || command.getStart_date() == null || CollectionUtils.isEmpty(command.getDow())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        if (command.getStart_date() < System.currentTimeMillis()) {
            throw new Exception(ExceptionEnum.start_date_not_allow);
        }
        ClassRoom classRoom = ClassRoom.builder()
                .name(command.getName())
                .course_id(command.getCourse_id())
                .shift_id(command.getShift_id())
                .max_student(command.getMax_student())
                .start_date(command.getStart_date())
                .dow(command.getDow())
                .status(command.getStatus())
                .build();
        logApplication.mongoDBConnection.insert(Log.builder()
                .class_name(MongodbEnum.collection_class_room)
                .action(Log.ACTION.add)
                .perform_by(command.getCurrent_member_id())
                .name(command.getName())
                .build());
        return mongoDBConnection.insert(classRoom);
    }

    public Optional<Paging<ClassRoom>> getList(CommandSearchClassRoom command) throws Exception {
        Map<String, Object> query = new HashMap<>();
        if (StringUtils.isNotBlank(command.getKeyword())) {
            Map<String, Object> $regex = new HashMap<>();
            $regex.put("$regex", Pattern.compile(command.getKeyword(), Pattern.CASE_INSENSITIVE));
            query.put("name", $regex);
        }
        if (!CollectionUtils.isEmpty(command.getShift_ids())) {
            query.put("shift_id", new org.bson.Document("$in", command.getShift_ids()));
        }
        if (!CollectionUtils.isEmpty(command.getCourse_ids())) {
            query.put("course_id", new org.bson.Document("$in", command.getCourse_ids()));
        }
        if (!CollectionUtils.isEmpty(command.getDow())) {
            query.put("dow", new org.bson.Document("$all", command.getDow()));
        }
        if (command.getStart_from_date() != null && command.getStart_to_date() != null) {
            query.put("start_date", new org.bson.Document("$gte", command.getStart_from_date()).append("$lte", command.getStart_to_date()));
        }
        if (!CollectionUtils.isEmpty(command.getStatus())) {
            query.put("status", new Document("$in", command.getStatus()));
        }
        return mongoDBConnection.find(query, command.getSort(), command.getPage(), command.getSize());
    }

    public Optional<ClassRoom> update(CommandAddClassRoom command) throws Exception {
        if (StringUtils.isBlank(command.getId())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        Optional<ClassRoom> optional = mongoDBConnection.getById(command.getId());
        if (!optional.isPresent()) {
            throw new Exception(ExceptionEnum.classroom_not_exist);
        }
        ClassRoom classRoom = optional.get();
        Map<String, Log.ChangeDetail> changeDetailMap = new HashMap<>();
        if (StringUtils.isNotBlank(command.getName()) && !command.getName().equals(classRoom.getName())) {
            changeDetailMap.put("name", Log.ChangeDetail.builder()
                    .old_value(classRoom.getName())
                    .new_value(command.getName())
                    .build());
            classRoom.setName(command.getName());
        }
        if (command.getMax_student() != null && !command.getMax_student().equals(classRoom.getMax_student())) {
            changeDetailMap.put("max_student", Log.ChangeDetail.builder()
                    .old_value(classRoom.getMax_student().toString())
                    .new_value(command.getMax_student().toString())
                    .build());
            classRoom.setMax_student(command.getMax_student());
        }
        if (command.getStatus() != null && !command.getStatus().equals(classRoom.getStatus())) {
            if (!ClassRoom.Status.create.equals(classRoom.getStatus())) {
                throw new Exception(ExceptionEnum.cannot_when_status_not_is_create);
            }
            changeDetailMap.put("status", Log.ChangeDetail.builder()
                    .old_value(classRoom.getStatus())
                    .new_value(command.getStatus())
                    .build());
            classRoom.setStatus(command.getStatus());
        }
        if (command.getStart_date() != null && !command.getStart_date().equals(classRoom.getStart_date())) {
            if (classRoom.getStart_date() < System.currentTimeMillis() || command.getStart_date() < System.currentTimeMillis()) {
                throw new Exception(ExceptionEnum.start_date_not_allow);
            }
            if (!ClassRoom.Status.register.equals(classRoom.getStatus())) {
                throw new Exception(ExceptionEnum.cannot_when_status_not_is_register);
            }
            scheduleApplication.validateScheduleExits(classRoom.get_id().toHexString());
            changeDetailMap.put("start_date", Log.ChangeDetail.builder()
                    .old_value(classRoom.getStart_date().toString())
                    .new_value(command.getStart_date().toString())
                    .build());
            classRoom.setStart_date(command.getStart_date());
        }
        logApplication.mongoDBConnection.insert(Log.builder()
                .class_name(MongodbEnum.collection_class_room)
                .action(Log.ACTION.update)
                .perform_by(command.getCurrent_member_id())
                .name(command.getName())
                .detail(changeDetailMap)
                .build());
        return mongoDBConnection.update(classRoom.get_id().toHexString(), classRoom);
    }

    public Optional<ClassRoom> getById(String id) {
        return mongoDBConnection.getById(id);
    }

    public Optional<List<ClassRoom>> getAll() {
        return mongoDBConnection.find(new HashMap<>());
    }

    public Optional<List<ClassRoom>> find(Map<String, Object> query) {
        return mongoDBConnection.find(query);
    }
}
