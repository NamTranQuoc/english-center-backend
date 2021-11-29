package com.englishcenter.category.course.application;

import com.englishcenter.category.course.CategoryCourse;
import com.englishcenter.category.course.command.CommandAddCategoryCourse;
import com.englishcenter.category.course.command.CommandGetCourseCategory;
import com.englishcenter.category.course.command.CommandSearchCategoryCourse;
import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.Paging;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.course.Course;
import com.englishcenter.course.command.CommandGetAllCourse;
import com.englishcenter.member.Member;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class CategoryCourseApplication implements ICategoryCourseApplication {
    public final MongoDBConnection<CategoryCourse> mongoDBConnection;

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
        CategoryCourse categoryCourse = CategoryCourse.builder()
                .name(command.getName())
                .status(command.getStatus())
                .description(command.getDescription())
                .create_date(System.currentTimeMillis())
                .build();
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
        if (StringUtils.isNotBlank(command.getName())) {
            categoryCourse.setName(command.getName());
        }
        if (StringUtils.isNotBlank(command.getDescription())) {
            categoryCourse.setDescription(command.getDescription());
        }
        if (StringUtils.isNotBlank(command.getStatus())) {
            categoryCourse.setStatus(command.getStatus());
        }
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
        return mongoDBConnection.find(new HashMap<>());
    }
}
