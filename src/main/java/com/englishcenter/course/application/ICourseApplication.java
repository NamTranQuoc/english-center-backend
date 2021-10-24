package com.englishcenter.course.application;

import com.englishcenter.core.utils.Paging;
import com.englishcenter.course.Course;
import com.englishcenter.course.command.CommandAddCourse;
import com.englishcenter.course.command.CommandSearchCourse;

import java.util.Optional;

public interface ICourseApplication {
    Optional<Course> add(CommandAddCourse command) throws Exception;

    Optional<Paging<Course>> getList(CommandSearchCourse command);

    Optional<Course> update(CommandAddCourse command) throws Exception;
}
