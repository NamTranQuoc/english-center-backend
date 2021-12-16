package com.englishcenter.shift.application;

import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.Paging;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.member.Member;
import com.englishcenter.shift.Shift;
import com.englishcenter.shift.command.CommandAddShift;
import com.englishcenter.shift.command.CommandSearchShift;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class ShiftApplication {
    public final MongoDBConnection<Shift> mongoDBConnection;

    @Autowired
    public ShiftApplication() {
        mongoDBConnection = new MongoDBConnection<>(MongodbEnum.collection_shift, Shift.class);
    }

    public Optional<Shift> add(CommandAddShift command) throws Exception {
        if(StringUtils.isAnyBlank(command.getName(), command.getFrom(), command.getTo())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        if (mongoDBConnection.checkExistByName(command.getName())) {
            throw new Exception(ExceptionEnum.shift_exist);
        }
        Shift shift = Shift.builder()
                .name(command.getName())
                .from(command.getFrom())
                .to(command.getTo())
                .build();
        return mongoDBConnection.insert(shift);
    }

    public Optional<Paging<Shift>> getList(CommandSearchShift command) throws Exception {
        Map<String, Object> query = new HashMap<>();
        if (StringUtils.isNotBlank(command.getKeyword())) {
            Map<String, Object> $regex = new HashMap<>();
            $regex.put("$regex", Pattern.compile(command.getKeyword(), Pattern.CASE_INSENSITIVE));
            query.put("name", $regex);
        }
        return mongoDBConnection.find(query, command.getSort(), command.getPage(), command.getSize());
    }

    public Optional<Shift> update(CommandAddShift command) throws Exception {
        if(StringUtils.isBlank(command.getId())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        Optional<Shift> optional = mongoDBConnection.getById(command.getId());
        if (!optional.isPresent()) {
            throw new Exception(ExceptionEnum.shift_not_exist);
        }

        Shift shift = optional.get();
        if (StringUtils.isNotBlank(command.getName()) && !command.getName().equals(shift.getName())) {
            if (mongoDBConnection.checkExistByName(command.getName())) {
                throw new Exception(ExceptionEnum.shift_exist);
            }
            shift.setName(command.getName());
        }
        if (StringUtils.isNotBlank(command.getFrom())) {
            shift.setFrom(command.getFrom());
        }
        if (StringUtils.isNotBlank(command.getTo())) {
            shift.setTo(command.getTo());
        }
        return mongoDBConnection.update(shift.get_id().toHexString(), shift);
    }

    public Optional<Shift> getById(String id) {
        return mongoDBConnection.getById(id);
    }

    public Optional<List<Shift>> getAll() {
        return mongoDBConnection.find(new HashMap<>());
    }
}
