package com.englishcenter.course;

import com.englishcenter.core.utils.ResponseDomain;
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

    @PostMapping("/course/get_list")
    public ResponseDomain getList(@RequestBody CommandSearchCourse command, @RequestParam Integer page, @RequestParam Integer size) {
        try {
            command.setPage(page);
            command.setSize(size);
            return this.outJsonV2(9999, null, courseApplication.getList(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @GetMapping("/course/get_all")
    public ResponseDomain getAll() {
        try {
            return this.outJsonV2(9999, null, courseApplication.getAll().orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @GetMapping("/course/get_by_study_program")
    public ResponseDomain getByStudyProgram(@RequestParam String id) {
        try {
            return this.outJsonV2(9999, null, courseApplication.getByStudyProgram(id).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/course/add")
    public ResponseDomain add(@RequestBody CommandAddCourse command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            command.setCurrent_member_id(this.getMemberId(Authorization));
            return this.outJsonV2(9999, null, courseApplication.add(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PutMapping("/course/update")
    public ResponseDomain update(@RequestBody CommandAddCourse command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            command.setCurrent_member_id(this.getMemberId(Authorization));
            return this.outJsonV2(9999, null, courseApplication.update(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @GetMapping("/course/get_by_status/{status}")
    public ResponseDomain getByStatus(@PathVariable String status) {
        try {
            return this.outJsonV2(9999, null, courseApplication.getCourseByStatus(status).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @GetMapping("/course/get_by_category_id/{id}")
    public ResponseDomain getByCategoryId(@PathVariable String id) {
        try {
            return this.outJsonV2(9999, null, courseApplication.getCourseByCategoryId(id).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }
}
