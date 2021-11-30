package com.englishcenter.course.application;

import com.englishcenter.category.course.CategoryCourse;
import com.englishcenter.category.course.application.ICategoryCourseApplication;
import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.Paging;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.course.Course;
import com.englishcenter.course.command.CommandAddCourse;
import com.englishcenter.course.command.CommandGetAllCourse;
import com.englishcenter.course.command.CommandSearchCourse;
import com.englishcenter.member.Member;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class CourseApplication implements ICourseApplication {
    public final MongoDBConnection<Course> mongoDBConnection;

    @Autowired
    private CourseApplication() {
        mongoDBConnection = new MongoDBConnection<>(MongodbEnum.collection_course, Course.class);
    }
    @Autowired
    private ICategoryCourseApplication categoryCourseApplication;

    @Override
    public Optional<Course> add(CommandAddCourse command) throws Exception {
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        if (StringUtils.isAnyBlank(command.getName(), command.getCategory_course_id()) || command.getInput_score() == null || command.getOutput_score() == null) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        Optional<CategoryCourse> categoryCourseOptional = categoryCourseApplication.getById(command.getCategory_course_id());
        if (!categoryCourseOptional.isPresent()) {
            throw new Exception(ExceptionEnum.category_course_not_exist);
        }
        Course course = Course.builder()
                .name(command.getName())
                .tuition(command.getTuition())
                .description(command.getDescription())
                .category_course_id(command.getCategory_course_id())
                .number_of_shift(command.getNumber_of_shift())
                .create_date(System.currentTimeMillis())
                .input_score(command.getInput_score())
                .output_score(command.getOutput_score())
                .status(command.getStatus())
                .build();
        return mongoDBConnection.insert(course);
    }

    @Override
    public Optional<Paging<Course>> getList(CommandSearchCourse command) {
        Map<String, Object> query = new HashMap<>();
        if (StringUtils.isNotBlank(command.getKeyword())) {
            Map<String, Object> $regex = new HashMap<>();
            $regex.put("$regex", Pattern.compile(command.getKeyword(), Pattern.CASE_INSENSITIVE));
            Map<String, Object> queryName = new HashMap<>();
            queryName.put("name", $regex);
            Map<String, Object> queryDescription = new HashMap<>();
            queryDescription.put("description", $regex);
            query.put("$or", Arrays.asList(queryName, queryDescription));
        }
        if (command.getFrom_date() != null && command.getTo_date() != null) {
            query.put("create_date", new Document("$gte", command.getFrom_date()).append("$lte", command.getTo_date()));
        }
        if (!CollectionUtils.isEmpty(command.getCategory_courses())) {
            query.put("category_course_id", new Document("$in", command.getCategory_courses()));
        }
        if (!CollectionUtils.isEmpty(command.getStatus())) {
            query.put("status", new Document("$in", command.getStatus()));
        }
        return mongoDBConnection.find(query, command.getSort(), command.getPage(), command.getSize());
    }

    @Override
    public Optional<List<CommandGetAllCourse>> getAll() {
        List<Course> list = mongoDBConnection.find(new HashMap<>()).orElse(new ArrayList<>());
        return Optional.of(list.stream().map(item -> CommandGetAllCourse.builder()
                ._id(item.get_id().toHexString())
                .name(item.getName())
                .build()).collect(Collectors.toList()));
    }

    @Override
    public Optional<List<CommandGetAllCourse>> getCourseByStatus(String status) {
        List<Course> list = mongoDBConnection.find(new Document("status", status)).orElse(new ArrayList<>());
        return Optional.of(list.stream().map(item -> CommandGetAllCourse.builder()
                ._id(item.get_id().toHexString())
                .name(item.getName())
                .build()).collect(Collectors.toList()));
    }

    @Override
    public Optional<Course> update(CommandAddCourse command) throws Exception {
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        if (StringUtils.isBlank(command.getId())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        Optional<Course> courseOptional = mongoDBConnection.getById(command.getId());
        if (!courseOptional.isPresent()) {
            throw new Exception(ExceptionEnum.course_not_exist);
        }
        Course course = courseOptional.get();
        if (StringUtils.isNotBlank(command.getName())) {
            course.setName(command.getName());
        }
        if (StringUtils.isNotBlank(command.getCategory_course_id())) {
            Optional<CategoryCourse> categoryCourse = categoryCourseApplication.getById(command.getCategory_course_id());
            if (categoryCourse.isPresent()) {
                course.setCategory_course_id(command.getCategory_course_id());
            } else {
                throw new Exception(ExceptionEnum.category_course_not_exist);
            }
        }
        if (StringUtils.isNotBlank(command.getDescription())) {
            course.setDescription(command.getDescription());
        }
        if (command.getNumber_of_shift() != null) {
            course.setNumber_of_shift(command.getNumber_of_shift());
        }
        if (command.getTuition() != null) {
            course.setTuition(command.getTuition());
        }
        if (command.getInput_score() != null) {
            course.setInput_score(command.getInput_score());
        }
        if (command.getOutput_score() != null) {
            course.setOutput_score(command.getOutput_score());
        }
        if (command.getStatus() != null) {
            course.setStatus(command.getStatus());
        }
        return mongoDBConnection.update(course.get_id().toHexString(), course);
    }

    @Override
    public Optional<Course> getById(String id) {
        return mongoDBConnection.getById(id);
    }
}
