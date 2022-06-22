package com.englishcenter.category.course.application;

import com.englishcenter.category.course.CategoryCourse;
import com.englishcenter.category.course.command.CommandAddCategoryCourse;
import com.englishcenter.category.course.command.CommandGetAllResponse;
import com.englishcenter.category.course.command.CommandGetCourseCategory;
import com.englishcenter.category.course.command.CommandSearchCategoryCourse;
import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.Paging;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.course.Course;
import com.englishcenter.course.application.CourseApplication;
import com.englishcenter.log.Log;
import com.englishcenter.log.LogApplication;
import com.englishcenter.member.Member;
import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class CategoryCourseApplication implements ICategoryCourseApplication {
    public final MongoDBConnection<CategoryCourse> mongoDBConnection;
    @Autowired
    private LogApplication logApplication;
    @Autowired
    private CourseApplication courseApplication;
    @Autowired
    public CategoryCourseApplication() {
        mongoDBConnection = new MongoDBConnection<>(MongodbEnum.collection_category_course, CategoryCourse.class);
    }

    @Override
    public Optional<CategoryCourse> add(CommandAddCategoryCourse command) throws Exception {
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        if (StringUtils.isAnyBlank(command.getName(), command.getStatus())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (mongoDBConnection.checkExistByName(command.getName())) {
            throw new Exception(ExceptionEnum.category_course_exist);
        }
        CategoryCourse categoryCourse = CategoryCourse.builder()
                .name(command.getName())
                .status(command.getStatus())
                .description(command.getDescription())
                .create_date(System.currentTimeMillis())
                .build();
        logApplication.mongoDBConnection.insert(Log.builder()
                .class_name(MongodbEnum.collection_category_course)
                .action(Log.ACTION.add)
                .perform_by(command.getCurrent_member_id())
                .name(command.getName())
                .build());
        return mongoDBConnection.insert(categoryCourse);
    }

    @Override
    public Optional<Paging<CategoryCourse>> getList(CommandSearchCategoryCourse command) {
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
        return mongoDBConnection.find(query, command.getSort(), command.getPage(), command.getSize());
    }

    @Override
    public Optional<CategoryCourse> update(CommandAddCategoryCourse command) throws Exception {
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        if (StringUtils.isBlank(command.getId())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        Optional<CategoryCourse> optional = mongoDBConnection.getById(command.getId());
        if (!optional.isPresent()) {
            throw new Exception(ExceptionEnum.category_course_not_exist);
        }
        CategoryCourse categoryCourse = optional.get();
        Map<String, Log.ChangeDetail> changeDetailMap = new HashMap<>();
        if (StringUtils.isNotBlank(command.getName()) && !command.getName().equals(categoryCourse.getName())) {
            if (mongoDBConnection.checkExistByName(command.getName())) {
                throw new Exception(ExceptionEnum.category_course_exist);
            }
            changeDetailMap.put("name", Log.ChangeDetail.builder()
                    .old_value(categoryCourse.getName())
                    .new_value(command.getName())
                    .build());
            categoryCourse.setName(command.getName());
        }
        if (StringUtils.isNotBlank(command.getDescription()) && !command.getDescription().equals(categoryCourse.getDescription())) {
            changeDetailMap.put("description", Log.ChangeDetail.builder()
                    .old_value(categoryCourse.getDescription())
                    .new_value(command.getDescription())
                    .build());
            categoryCourse.setDescription(command.getDescription());
        }
        if (StringUtils.isNotBlank(command.getStatus()) && !command.getStatus().equals(categoryCourse.getStatus())) {
            changeDetailMap.put("status", Log.ChangeDetail.builder()
                    .old_value(categoryCourse.getStatus())
                    .new_value(command.getStatus())
                    .build());
            categoryCourse.setStatus(command.getStatus());
        }
        logApplication.mongoDBConnection.insert(Log.builder()
                .class_name(MongodbEnum.collection_category_course)
                .action(Log.ACTION.update)
                .perform_by(command.getCurrent_member_id())
                .name(command.getName())
                .detail(changeDetailMap)
                .build());
        return mongoDBConnection.update(categoryCourse.get_id().toHexString(), categoryCourse);
    }

    @Override
    public Optional<List<CommandGetCourseCategory>> getCourseCategoryByStatus(String status) {
        List<CategoryCourse> list = mongoDBConnection.find(new Document("status", status)).orElse(new ArrayList<>());
        return Optional.of(list.stream().map(item -> CommandGetCourseCategory.builder()
                ._id(item.get_id().toHexString())
                .name(item.getName())
                .build()).collect(Collectors.toList()));
    }

    @Override
    public Optional<CategoryCourse> getById(String id) {
        return mongoDBConnection.getById(id);
    }

    @Override
    public Optional<List<CategoryCourse>> getAll() {
        Optional<List<CategoryCourse>> categoryCourses = mongoDBConnection.find(new HashMap<>());
        if (categoryCourses.isPresent()) {
            List<BasicDBObject> aggregate = Arrays.asList(
                    BasicDBObject.parse("{\"$match\": {\"status\": \"active\"}}"),
                    BasicDBObject.parse("{\"$group\": {_id: \"$category_course_id\", \"count\": {\"$sum\": 1}}}")
            );
            AggregateIterable<Document> documents = courseApplication.mongoDBConnection.aggregate(aggregate);
            Map<String, Integer> count = new HashMap<>();
            if (documents != null) {
                for (Document item : documents) {
                    if (item.containsKey("_id") && item.get("_id") != null && StringUtils.isNotBlank(item.get("_id").toString())) {
                        count.put(item.get("_id").toString(), item.getInteger("count", 0));
                    }
                }
            }
            for (CategoryCourse c: categoryCourses.get()) {
                c.setNumber_of_course(count.getOrDefault(c.get_id().toHexString(), 0));
            }
        }
        return categoryCourses;
    }

    @Override
    public Optional<List<CommandGetAllResponse>> view() {
        List<CategoryCourse> categoryCourses = mongoDBConnection.find(new Document("status", "active")).orElse(new ArrayList<>());
        Map<String, CommandGetAllResponse> result = new HashMap<>();
        for (CategoryCourse categoryCourse : categoryCourses) {
            result.put(categoryCourse.get_id().toHexString(), CommandGetAllResponse.builder()
                    .courses(new ArrayList<>())
                    .name(categoryCourse.getName())
                    .build());
        }
        List<Course> courses = courseApplication.mongoDBConnection.find(new Document("status", "active")).orElse(new ArrayList<>());
        for (Course course : courses) {
            result.get(course.getCategory_course_id()).getCourses().add(CommandGetAllResponse.Course.builder()
                    .id(course.get_id().toHexString())
                    .input_score(course.getInput_score())
                    .output_score(course.getOutput_score())
                    .name(course.getName())
                    .number_of_shift(course.getNumber_of_shift())
                    .tuition(course.getTuition())
                    .build());
        }
        List<CommandGetAllResponse> commandGetAllResponses = new ArrayList<>();
        result.forEach((k, v) -> {
            if (v.getCourses().size() != 0) {
                commandGetAllResponses.add(v);
            }
        });
        return Optional.of(commandGetAllResponses);
    }
}
