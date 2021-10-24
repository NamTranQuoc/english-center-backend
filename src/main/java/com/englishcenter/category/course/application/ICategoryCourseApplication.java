package com.englishcenter.category.course.application;

import com.englishcenter.category.course.CategoryCourse;
import com.englishcenter.category.course.command.CommandAddCategoryCourse;
import com.englishcenter.category.course.command.CommandSearchCategoryCourse;
import com.englishcenter.core.utils.Paging;

import java.util.Optional;

public interface ICategoryCourseApplication {
    Optional<CategoryCourse> add(CommandAddCategoryCourse command) throws Exception;

    Optional<Paging<CategoryCourse>> getList(CommandSearchCategoryCourse command);

    Optional<CategoryCourse> update(CommandAddCategoryCourse command) throws Exception;

    Optional<CategoryCourse> getById(String id);
}
