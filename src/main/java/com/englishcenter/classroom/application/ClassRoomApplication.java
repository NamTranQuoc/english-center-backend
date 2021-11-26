package com.englishcenter.classroom.application;

import com.englishcenter.classroom.ClassRoom;
import com.englishcenter.classroom.command.CommandAddClassRoom;
import com.englishcenter.classroom.command.CommandSearchClassRoom;
import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.Paging;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.member.Member;
import org.apache.commons.lang3.StringUtils;
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
                .build();
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
        if (StringUtils.isNotBlank(command.getName())) {
            classRoom.setName(command.getName());
        }
        if (command.getMax_student() != null) {
            classRoom.setMax_student(command.getMax_student());
        }
        if (command.getStart_date() != null) {
            if (classRoom.getStart_date() < System.currentTimeMillis() || command.getStart_date() < System.currentTimeMillis()) {
                throw new Exception(ExceptionEnum.start_date_not_allow);
            }
            classRoom.setStart_date(command.getStart_date());
        }
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
