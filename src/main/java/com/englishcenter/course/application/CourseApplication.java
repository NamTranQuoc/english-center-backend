package com.englishcenter.course.application;

import com.englishcenter.category.course.CategoryCourse;
import com.englishcenter.category.course.application.ICategoryCourseApplication;
import com.englishcenter.classroom.application.ClassRoomApplication;
import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.Paging;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.course.Course;
import com.englishcenter.course.command.CommandAddCourse;
import com.englishcenter.course.command.CommandGetAllCourse;
import com.englishcenter.course.command.CommandSearchCourse;
import com.englishcenter.log.Log;
import com.englishcenter.log.LogApplication;
import com.englishcenter.member.Member;
import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
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
    private ICategoryCourseApplication categoryCourseApplication;
    @Autowired
    private ClassRoomApplication classRoomApplication;
    @Autowired
    private LogApplication logApplication;
    @Autowired
    private CourseApplication() {
        mongoDBConnection = new MongoDBConnection<>(MongodbEnum.collection_course, Course.class);
    }

    @Override
    public Optional<Course> add(CommandAddCourse command) throws Exception {
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        if (StringUtils.isAnyBlank(command.getName(), command.getCategory_course_id()) || command.getInput_score() == null) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        Optional<CategoryCourse> categoryCourseOptional = categoryCourseApplication.getById(command.getCategory_course_id());
        if (!categoryCourseOptional.isPresent()) {
            throw new Exception(ExceptionEnum.category_course_not_exist);
        }
        if (mongoDBConnection.checkExistByName(command.getName())) {
            throw new Exception(ExceptionEnum.course_exist);
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
        logApplication.mongoDBConnection.insert(Log.builder()
                .class_name(MongodbEnum.collection_course)
                .action(Log.ACTION.add)
                .perform_by(command.getCurrent_member_id())
                .name(command.getName())
                .build());
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
        List<CommandGetAllCourse> courses = list.stream().map(item -> CommandGetAllCourse.builder()
                ._id(item.get_id().toHexString())
                .name(item.getName())
                .build()).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(list)) {
            List<BasicDBObject> aggregate = Arrays.asList(
                    BasicDBObject.parse("{\"$match\": {\"status\": \"register\"}}"),
                    BasicDBObject.parse("{\"$group\": {_id: \"$course_id\", \"count\": {\"$sum\": 1}}}")
            );
            AggregateIterable<Document> documents = classRoomApplication.mongoDBConnection.aggregate(aggregate);
            Map<String, Integer> count = new HashMap<>();
            if (documents != null) {
                for (Document item : documents) {
                    if (item.containsKey("_id") && item.get("_id") != null && StringUtils.isNotBlank(item.get("_id").toString())) {
                        count.put(item.get("_id").toString(), item.getInteger("count", 0));
                    }
                }
            }
            for (CommandGetAllCourse c: courses) {
                c.setNumber_of_class(count.getOrDefault(c.get_id(), 0));
            }
        }
        return Optional.of(courses);
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
    public Optional<List<CommandGetAllCourse>> getCourseByCategoryId(String id) {
        List<Course> list = mongoDBConnection.find(new Document("category_course_id", id).append("status", "active")).orElse(new ArrayList<>());
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
        Map<String, Log.ChangeDetail> changeDetailMap = new HashMap<>();
        if (StringUtils.isNotBlank(command.getName()) && !command.getName().equals(course.getName())) {
            if (mongoDBConnection.checkExistByName(command.getName())) {
                throw new Exception(ExceptionEnum.course_exist);
            }
            changeDetailMap.put("name", Log.ChangeDetail.builder()
                    .old_value(course.getName())
                    .new_value(command.getName())
                    .build());
            course.setName(command.getName());
        }
        if (StringUtils.isNotBlank(command.getCategory_course_id()) && !command.getCategory_course_id().equals(course.getCategory_course_id())) {
            Optional<CategoryCourse> categoryCourse = categoryCourseApplication.getById(command.getCategory_course_id());
            if (categoryCourse.isPresent()) {
                changeDetailMap.put("category_course_id", Log.ChangeDetail.builder()
                        .old_value(course.getCategory_course_id())
                        .new_value(command.getCategory_course_id())
                        .build());
                course.setCategory_course_id(command.getCategory_course_id());
            } else {
                throw new Exception(ExceptionEnum.category_course_not_exist);
            }
        }
        if (StringUtils.isNotBlank(command.getDescription()) && !command.getDescription().equals(course.getDescription())) {
            changeDetailMap.put("description", Log.ChangeDetail.builder()
                    .old_value(course.getDescription())
                    .new_value(command.getDescription())
                    .build());
            course.setDescription(command.getDescription());
        }
        if (command.getNumber_of_shift() != null && !command.getNumber_of_shift().equals(course.getNumber_of_shift())) {
            changeDetailMap.put("number_of_shift", Log.ChangeDetail.builder()
                    .old_value(course.getNumber_of_shift().toString())
                    .new_value(command.getNumber_of_shift().toString())
                    .build());
            course.setNumber_of_shift(command.getNumber_of_shift());
        }
        if (command.getTuition() != null && !command.getTuition().equals(course.getTuition())) {
            changeDetailMap.put("tuition", Log.ChangeDetail.builder()
                    .old_value(course.getTuition().toString())
                    .new_value(command.getTuition().toString())
                    .build());
            course.setTuition(command.getTuition());
        }
        if (command.getInput_score() != null && !command.getInput_score().equals(course.getInput_score())) {
            changeDetailMap.put("input_score", Log.ChangeDetail.builder()
                    .old_value(course.getInput_score().toString())
                    .new_value(command.getInput_score().toString())
                    .build());
            course.setInput_score(command.getInput_score());
        }
        if (command.getOutput_score() != null && !command.getOutput_score().equals(course.getOutput_score())) {
            changeDetailMap.put("output_score", Log.ChangeDetail.builder()
                    .old_value(course.getOutput_score().toString())
                    .new_value(command.getOutput_score().toString())
                    .build());
            course.setOutput_score(command.getOutput_score());
        }
        if (StringUtils.isNotBlank(command.getStatus()) && !command.getStatus().equals(course.getStatus())) {
            changeDetailMap.put("status", Log.ChangeDetail.builder()
                    .old_value(course.getStatus())
                    .new_value(command.getStatus())
                    .build());
            course.setStatus(command.getStatus());
        }
        logApplication.mongoDBConnection.insert(Log.builder()
                .class_name(MongodbEnum.collection_class_room)
                .action(Log.ACTION.update)
                .perform_by(command.getCurrent_member_id())
                .name(command.getName())
                .detail(changeDetailMap)
                .build());
        return mongoDBConnection.update(course.get_id().toHexString(), course);
    }

    @Override
    public Optional<Course> getById(String id) {
        return mongoDBConnection.getById(id);
    }

    @Override
    public Optional<List<CommandGetAllCourse>> getByStudyProgram(String id) {
        Map<String, Object> query = new HashMap<>();
        query.put("status", "active");
        query.put("category_course_id", id);
        List<Course> list = mongoDBConnection.find(query).orElse(new ArrayList<>());
        List<CommandGetAllCourse> courses = list.stream().map(item -> CommandGetAllCourse.builder()
                ._id(item.get_id().toHexString())
                .name(item.getName())

                .tuition(item.getTuition())
                .build()).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(list)) {
            StringBuilder ids = new StringBuilder();
            for (Course c: list) {
                ids.append("\"").append(c.get_id().toHexString()).append("\"").append(",");
            }
            if (!"".equals(ids.toString())) {
                ids = new StringBuilder(ids.substring(0, ids.length() - 1));
            }

            List<BasicDBObject> aggregate = Arrays.asList(
                    BasicDBObject.parse("{\"$match\": {\"status\": \"register\", \"course_id\": {\"$in\": [" + ids + "]}}}"),
                    BasicDBObject.parse("{\"$group\": {_id: \"$course_id\", \"count\": {\"$sum\": 1}}}")
            );
            AggregateIterable<Document> documents = classRoomApplication.mongoDBConnection.aggregate(aggregate);
            Map<String, Integer> count = new HashMap<>();
            if (documents != null) {
                for (Document item : documents) {
                    if (item.containsKey("_id") && item.get("_id") != null && StringUtils.isNotBlank(item.get("_id").toString())) {
                        count.put(item.get("_id").toString(), item.getInteger("count", 0));
                    }
                }
            }
            for (CommandGetAllCourse c: courses) {
                c.setNumber_of_class(count.getOrDefault(c.get_id(), 0));
            }
        }
        return Optional.of(courses);
    }
}
