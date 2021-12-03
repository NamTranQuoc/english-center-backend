package com.englishcenter.category.course.application;

import com.englishcenter.category.course.CategoryCourse;
import com.englishcenter.category.course.command.CommandAddCategoryCourse;
import com.englishcenter.category.course.command.CommandGetAllResponse;
import com.englishcenter.category.course.command.CommandGetCourseCategory;
import com.englishcenter.category.course.command.CommandSearchCategoryCourse;
import com.englishcenter.core.utils.Paging;

import java.util.List;
import java.util.Optional;

public interface ICategoryCourseApplication {
    Optional<CategoryCourse> add(CommandAddCategoryCourse command) throws Exception;

    Optional<Paging<CategoryCourse>> getList(CommandSearchCategoryCourse command);

    Optional<CategoryCourse> update(CommandAddCategoryCourse command) throws Exception;

    Optional<List<CommandGetCourseCategory>> getCourseCategoryByStatus(String status);

    Optional<CategoryCourse> getById(String id);

    Optional<List<CategoryCourse>> getAll();

    Optional<List<CommandGetAllResponse>> view();
}
