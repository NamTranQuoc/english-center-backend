package com.englishcenter.exam.schedule.application;

import com.englishcenter.classroom.ClassRoom;
import com.englishcenter.classroom.command.CommandSearchClassRoom;
import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.Paging;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.exam.schedule.ExamSchedule;
import com.englishcenter.exam.schedule.command.CommandAddExamSchedule;
import com.englishcenter.exam.schedule.command.CommandSearchExamSchedule;
import com.englishcenter.member.Member;
import com.englishcenter.room.Room;
import com.englishcenter.room.command.CommandAddRoom;
import com.englishcenter.room.command.CommandSearchRoom;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.regex.Pattern;
@Component
public class ExamScheduleApplication {
    public final MongoDBConnection<ExamSchedule> mongoDBConnection;

    @Autowired
    public ExamScheduleApplication() {
        mongoDBConnection = new MongoDBConnection<>(MongodbEnum.collection_exam_schedule, ExamSchedule.class);
    }
    public Optional<ExamSchedule> add(CommandAddExamSchedule command) throws Exception {
        if(StringUtils.isAnyBlank(command.getRoom_id())
                || command.getStart_time() == null
                || command.getEnd_time() == null
                || command.getMember_ids().isEmpty()
                || command.getMin_quantity() == null
                || command.getMax_quantity() == null) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        ExamSchedule examSchedule = ExamSchedule.builder()
                .start_time(command.getStart_time())
                .end_time(command.getEnd_time())
                .room_id(command.getRoom_id())
                .member_ids(command.getMember_ids())
                .min_quantity(command.getMin_quantity())
                .max_quantity(command.getMax_quantity())
                .build();
        return mongoDBConnection.insert(examSchedule);
    }

    public Optional<Paging<ExamSchedule>> getList(CommandSearchExamSchedule command) throws Exception {
        Map<String, Object> query = new HashMap<>();
        if (StringUtils.isNotBlank(command.getKeyword())) {
            Map<String, Object> $regex = new HashMap<>();
            $regex.put("$regex", Pattern.compile(command.getKeyword(), Pattern.CASE_INSENSITIVE));
            query.put("name", $regex);
        }
        if (!CollectionUtils.isEmpty(command.getMember_ids())) {
            query.put("member_ids", new org.bson.Document("$in", command.getMember_ids()));
        }
        if (command.getRoom_id() != null) {
            query.put("room_id", new org.bson.Document("$in", command.getRoom_id()));
        }
        if (command.getStart_time() != null && command.getEnd_time() != null) {
            query.put("start_time", new org.bson.Document("$gte", command.getStart_time()).append("$lte", command.getEnd_time()));
        }
        return mongoDBConnection.find(query, command.getSort(), command.getPage(), command.getSize());
    }

    public Optional<ExamSchedule> update(CommandAddExamSchedule command) throws Exception {
        if(StringUtils.isBlank(command.getId())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        Optional<ExamSchedule> optional = mongoDBConnection.getById(command.getId());
        if (!optional.isPresent()) {
            throw new Exception(ExceptionEnum.exam_schedule_not_exist);
        }

        ExamSchedule examSchedule = optional.get();
        if (StringUtils.isNotBlank(command.getRoom_id())) {
            examSchedule.setRoom_id(command.getRoom_id());
        }
        if (command.getStart_time() != null) {
            examSchedule.setStart_time(command.getStart_time());
        }
        if (command.getEnd_time() != null) {
            examSchedule.setEnd_time(command.getEnd_time());
        }
        if (command.getMax_quantity() != null) {
            examSchedule.setMax_quantity(command.getMax_quantity());
        }
        if (command.getMin_quantity() != null) {
            examSchedule.setMin_quantity(command.getMin_quantity());
        }
        if (!CollectionUtils.isEmpty(command.getMember_ids())) {
            examSchedule.setMember_ids(command.getMember_ids());
        }
        return mongoDBConnection.update(examSchedule.get_id().toHexString(), examSchedule);
    }

    public Optional<List<ExamSchedule>> find(Map<String, Object> query, Map<String, Object> sort) {
        return mongoDBConnection.find(query, sort);
    }

    public Optional<List<ExamSchedule>> getAll() {
        return mongoDBConnection.find(new HashMap<>());
    }

    public Optional<ExamSchedule> getById(String id) {
        return mongoDBConnection.getById(id);
    }
}
