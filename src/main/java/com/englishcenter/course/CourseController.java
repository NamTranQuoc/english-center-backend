package com.englishcenter.course;

import com.englishcenter.core.utils.ResponseUtils;
import com.englishcenter.course.application.ICourseApplication;
import com.englishcenter.course.command.CommandAddCourse;
import com.englishcenter.course.command.CommandSearchCourse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@Component
@RestController(value = "/course")
public class CourseController extends ResponseUtils {
    @Autowired
    private ICourseApplication courseApplication;

    @GetMapping("/course/get_list")
    public String getList(@RequestBody CommandSearchCourse command, @RequestParam Integer page, @RequestParam Integer size) {
        try {
            command.setPage(page);
            command.setSize(size);
            return this.outJson(9999, null, courseApplication.getList(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/course/add")
    public String add(@RequestBody CommandAddCourse command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            return this.outJson(9999, null, courseApplication.add(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @PutMapping("/course/update")
    public String update(@RequestBody CommandAddCourse command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            return this.outJson(9999, null, courseApplication.update(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }
}