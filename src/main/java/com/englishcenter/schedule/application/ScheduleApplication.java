package com.englishcenter.schedule.application;

import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.Paging;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.member.Member;
import com.englishcenter.schedule.Schedule;
import com.englishcenter.schedule.command.CommandAddSchedule;
import com.englishcenter.schedule.command.CommandSearchSchedule;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class ScheduleApplication {
    public final MongoDBConnection<Schedule> mongoDBConnection;

    @Autowired
    public ScheduleApplication() {
        mongoDBConnection = new MongoDBConnection<>(MongodbEnum.collection_schedule, Schedule.class);
    }

    public Optional<Schedule> add(CommandAddSchedule command) throws Exception {
        if(StringUtils.isAnyBlank(command.getClassroom_id())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        Optional<Class> optional = mongoDBConnection.
        Schedule schedule = Schedule.builder()
                .name(command.getName())
                .from(command.getFrom())
                .to(command.getTo())
                .build();
        return mongoDBConnection.insert(schedule);
    }

    public Optional<Paging<Schedule>> getList(CommandSearchSchedule command) throws Exception {
        Map<String, Object> query = new HashMap<>();
        if (StringUtils.isNotBlank(command.getKeyword())) {
            Map<String, Object> $regex = new HashMap<>();
            $regex.put("$regex", Pattern.compile(command.getKeyword(), Pattern.CASE_INSENSITIVE));
            query.put("name", $regex);
        }
        return mongoDBConnection.find(query, command.getSort(), command.getPage(), command.getSize());
    }

    public Optional<Schedule> update(CommandAddSchedule command) throws Exception {
        if(StringUtils.isBlank(command.getId())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        Optional<Schedule> optional = mongoDBConnection.getById(command.getId());
        if (!optional.isPresent()) {
            throw new Exception(ExceptionEnum.shift_not_exist);
        }

        Schedule schedule = optional.get();
        if (StringUtils.isNotBlank(command.getName())) {
            schedule.setName(command.getName());
        }
        if (StringUtils.isNotBlank(command.getFrom())) {
            schedule.setFrom(command.getFrom());
        }
        if (StringUtils.isNotBlank(command.getTo())) {
            schedule.setTo(command.getTo());
        }
        return mongoDBConnection.update(schedule.get_id().toHexString(), schedule);
    }

    public Optional<Schedule> getById(String id) {
        return mongoDBConnection.getById(id);
    }

    public Optional<List<Schedule>> getAll() {
        return mongoDBConnection.find(new HashMap<>());
    }
}
