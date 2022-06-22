package com.englishcenter.course.application;

import com.englishcenter.category.course.command.CommandGetAllResponse;
import com.englishcenter.core.utils.Paging;
import com.englishcenter.course.Course;
import com.englishcenter.course.command.CommandAddCourse;
import com.englishcenter.course.command.CommandGetAllCourse;
import com.englishcenter.course.command.CommandSearchCourse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface ICourseApplication {
    Optional<Course> add(CommandAddCourse command) throws Exception;

    Optional<Paging<Course>> getList(CommandSearchCourse command);

    Optional<List<CommandGetAllCourse>> getAll();

    Optional<List<CommandGetAllCourse>> getCourseByStatus(String status);

    Optional<List<CommandGetAllCourse>> getCourseByCategoryId(String Id);

    Optional<Course> update(CommandAddCourse command) throws Exception;

    Optional<Course> getById(String id);

    Optional<List<CommandGetAllCourse>> getByStudyProgram(String id);

    Optional<List<CommandGetAllResponse.Course>> getCourseSuggest(String memberId);
}
