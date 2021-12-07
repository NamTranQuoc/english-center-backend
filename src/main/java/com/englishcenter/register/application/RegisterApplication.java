package com.englishcenter.register.application;

import com.englishcenter.classroom.ClassRoom;
import com.englishcenter.classroom.application.ClassRoomApplication;
import com.englishcenter.core.utils.Paging;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.course.Course;
import com.englishcenter.course.application.CourseApplication;
import com.englishcenter.member.Member;
import com.englishcenter.member.application.MemberApplication;
import com.englishcenter.register.command.CommandAddRegister;
import com.englishcenter.register.command.CommandGetListRegister;
import com.englishcenter.register.command.CommandGetListResponse;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class RegisterApplication {
    @Autowired
    private ClassRoomApplication classRoomApplication;
    @Autowired
    private MemberApplication memberApplication;
    @Autowired
    private CourseApplication courseApplication;

    public Optional<ClassRoom> add(CommandAddRegister command) throws Exception {
        if (StringUtils.isAnyBlank(command.getClass_id(), command.getStudent_id(), command.getStatus())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }

        Optional<ClassRoom> optionalClassRoom = classRoomApplication.getById(command.getClass_id());
        if (!optionalClassRoom.isPresent()) {
            throw new Exception(ExceptionEnum.classroom_not_exist);
        }
        ClassRoom classRoom = optionalClassRoom.get();

        Optional<Course> optionalCourse = courseApplication.getById(classRoom.getCourse_id());
        if (!optionalCourse.isPresent()) {
            throw new Exception(ExceptionEnum.course_not_exist);
        }
        Course course = optionalCourse.get();

        Map<String, Object> query = new HashMap<>();
        query.put("$or", Arrays.asList(
                new Document("email", command.getStudent_id()),
                new Document("code", command.getStudent_id()),
                new Document("phone_number", command.getStudent_id())
        ));
        query.put("status", Member.MemberStatus.ACTIVE);
        Optional<Member> optionalMember = memberApplication.mongoDBConnection.findOne(query);
        if (!optionalMember.isPresent()) {
            throw new Exception(ExceptionEnum.member_not_exist);
        }
        Member member = optionalMember.get();

        if (classRoom.getMax_student() <= classRoom.getStudent_ids().size()) {
            throw new Exception(ExceptionEnum.class_full);
        }

        if (member.getCurrent_score().getTotal() < course.getInput_score()) {
            throw new Exception(ExceptionEnum.input_score_not_enough);
        }
        classRoom.getStudent_ids().add(ClassRoom.StudentRegister.builder()
                .status(command.getStatus())
                .student_id(member.get_id().toHexString())
                .update_by(command.getCurrent_member())
                .update_date(System.currentTimeMillis())
                .amount_paid(ClassRoom.RegisterStatus.paid.equals(command.getStatus()) ? course.getTuition() : 0L)
                .build());
        return classRoomApplication.mongoDBConnection.update(classRoom.get_id().toHexString(), classRoom);
    }

    public Optional<Paging<CommandGetListResponse>> getList(CommandGetListRegister command) throws Exception {
        Map<String, Object> query = new HashMap<>();
        if (StringUtils.isNotBlank(command.getKeyword())) {
            Map<String, Object> $regex = new HashMap<>();
            $regex.put("$regex", Pattern.compile(command.getKeyword(), Pattern.CASE_INSENSITIVE));

            Map<String, Object> queryMember = new HashMap<>();
            queryMember.put("$or", Arrays.asList(
                    new Document("email", $regex),
                    new Document("code", $regex),
                    new Document("phone_number", $regex),
                    new Document("name", $regex)
            ));
            List<String> ids = memberApplication.find(queryMember).orElse(new ArrayList<>())
                    .stream().map(item -> item.get_id().toHexString()).collect(Collectors.toList());
            query.put("student_ids.student_id", new Document("$in", ids));
        }
        Optional<ClassRoom> classRoom = classRoomApplication.mongoDBConnection.getById(command.getClass_id());
        if (!classRoom.isPresent()) {
            return Optional.empty();
        }
        List<ObjectId> ids = classRoom.get().getStudent_ids().stream().map(item -> new ObjectId(item.getStudent_id())).collect(Collectors.toList());
        Map<String, Object> queryMember = new HashMap<>();
        queryMember.put("_id", new Document("$in", ids));
        List<Member> memberList = memberApplication.mongoDBConnection.findList(queryMember, command.getSort(), command.getPage(), command.getSize()).orElse(new ArrayList<>());

        List<CommandGetListResponse> commandGetListResponseList = memberList.stream().map(item -> CommandGetListResponse.builder()
                .member(item)
                .class_id(classRoom.get().get_id().toHexString())
                .register(getRegisByMemberId(item.get_id().toHexString(), classRoom.get().getStudent_ids()))
                .build()).collect(Collectors.toList());
        return Optional.of(Paging.<CommandGetListResponse>builder()
                .items(commandGetListResponseList)
                .total_items((long) commandGetListResponseList.size())
                .build());
    }

    private ClassRoom.StudentRegister getRegisByMemberId(String id, List<ClassRoom.StudentRegister> studentRegisterList) {
        for (ClassRoom.StudentRegister item : studentRegisterList) {
            if (item.getStudent_id().equals(id)) {
                return item;
            }
        }
        return null;
    }

    public Optional<ClassRoom> update(CommandAddRegister command) throws Exception {
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        if (StringUtils.isAnyBlank(command.getStudent_id(), command.getStatus(), command.getClass_id())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        Optional<ClassRoom> optionalClassRoom = classRoomApplication.getById(command.getClass_id());
        if (!optionalClassRoom.isPresent()) {
            throw new Exception(ExceptionEnum.classroom_not_exist);
        }
        ClassRoom classRoom = optionalClassRoom.get();
        Optional<Course> optionalCourse = courseApplication.getById(classRoom.getCourse_id());
        if (!optionalCourse.isPresent()) {
            throw new Exception(ExceptionEnum.course_not_exist);
        }
        Course course = optionalCourse.get();
        if (StringUtils.isNotBlank(command.getStatus())) {
            for (ClassRoom.StudentRegister studentRegister : classRoom.getStudent_ids()) {
                if (studentRegister.getStudent_id().equals(command.getStudent_id())) {
                    studentRegister.setStatus(command.getStatus());
                    studentRegister.setAmount_paid(ClassRoom.RegisterStatus.paid.equals(command.getStatus()) ? course.getTuition() : 0L);
                    studentRegister.setUpdate_by(command.getCurrent_member_id());
                    studentRegister.setUpdate_date(System.currentTimeMillis());
                }
            }
        }
        return classRoomApplication.mongoDBConnection.update(classRoom.get_id().toHexString(), classRoom);
    }

    public Optional<ClassRoom> delete(CommandAddRegister command) throws Exception {
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        if (StringUtils.isAnyBlank(command.getStudent_id(), command.getClass_id())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        Optional<ClassRoom> optionalClassRoom = classRoomApplication.mongoDBConnection.getById(command.getClass_id());
        if (!optionalClassRoom.isPresent()) {
            throw new Exception(ExceptionEnum.classroom_not_exist);
        }
        ClassRoom classRoom = optionalClassRoom.get();
        classRoom.getStudent_ids().removeIf(studentRegister -> studentRegister.getStudent_id().equals(command.getStudent_id()));
        return classRoomApplication.mongoDBConnection.update(classRoom.get_id().toHexString(), classRoom);
    }
}
