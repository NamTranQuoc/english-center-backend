package com.englishcenter.register.application;

import com.englishcenter.classroom.ClassRoom;
import com.englishcenter.classroom.command.CommandAddClassRoom;
import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.member.Member;
import com.englishcenter.register.Register;
import com.englishcenter.register.command.CommandAddRegister;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Optional;

@Component
public class RegisterApplication {
    public final MongoDBConnection<Register> mongoDBConnection;

    @Autowired
    public RegisterApplication() {
        mongoDBConnection = new MongoDBConnection<>(MongodbEnum.collection_register, Register.class);
    }

    public Optional<Register> add(CommandAddRegister command) throws Exception {
        if (StringUtils.isAnyBlank(command.getClass_id(), command.getStudent_id(), command.getStatus())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }

        Register register = Register.builder()
                .name(command.getName())
                .course_id(command.getCourse_id())
                .shift_id(command.getShift_id())
                .max_student(command.getMax_student())
                .start_date(command.getStart_date())
                .dow(command.getDow())
                .status(command.getStatus())
                .build();
        return mongoDBConnection.insert(classRoom);
    }
}
