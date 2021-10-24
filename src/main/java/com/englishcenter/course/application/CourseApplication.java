package com.englishcenter.course.application;

import com.englishcenter.category.course.CategoryCourse;
import com.englishcenter.category.course.application.ICategoryCourseApplication;
import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.Paging;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.course.Course;
import com.englishcenter.course.command.CommandAddCourse;
import com.englishcenter.course.command.CommandSearchCourse;
import com.englishcenter.member.Member;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

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
        if (Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        if (StringUtils.isAnyBlank(command.getName(), command.getCategory_course_id())) {
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
        return mongoDBConnection.find(query, command.getSort(), command.getPage(), command.getSize());
    }

    @Override
    public Optional<Course> update(CommandAddCourse command) throws Exception {
        if (Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        if (StringUtils.isNotBlank(command.getId())) {
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
        return mongoDBConnection.update(course.get_id().toHexString(), course);
    }
}
