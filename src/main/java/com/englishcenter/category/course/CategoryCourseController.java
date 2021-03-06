package com.englishcenter.category.course;

import com.englishcenter.category.course.application.ICategoryCourseApplication;
import com.englishcenter.category.course.command.CommandAddCategoryCourse;
import com.englishcenter.category.course.command.CommandSearchCategoryCourse;
import com.englishcenter.core.utils.ResponseDomain;
import com.englishcenter.core.utils.ResponseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@Component
@RestController(value = "/category_course")
public class CategoryCourseController extends ResponseUtils {
    @Autowired
    private ICategoryCourseApplication categoryCourseApplication;

    @GetMapping("/category_course/get_all")
    public ResponseDomain getAll() {
        try {
            return this.outJsonV2(9999, null, categoryCourseApplication.getAll().orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @GetMapping("/category_course/view")
    public ResponseDomain view() {
        try {
            return this.outJsonV2(9999, null, categoryCourseApplication.view().orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/category_course/get_list")
    public ResponseDomain getList(@RequestBody CommandSearchCategoryCourse command, @RequestParam Integer page, @RequestParam Integer size) {
        try {
            command.setPage(page);
            command.setSize(size);
            return this.outJsonV2(9999, null, categoryCourseApplication.getList(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/category_course/add")
    public ResponseDomain add(@RequestBody CommandAddCategoryCourse command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            command.setCurrent_member_id(this.getMemberId(Authorization));
            return this.outJsonV2(9999, null, categoryCourseApplication.add(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PutMapping("/category_course/update")
    public ResponseDomain update(@RequestBody CommandAddCategoryCourse command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            command.setCurrent_member_id(this.getMemberId(Authorization));
            return this.outJsonV2(9999, null, categoryCourseApplication.update(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @GetMapping("/category_course/get_by_status/{status}")
    public ResponseDomain getByStatus(@PathVariable String status) {
        try {
            return this.outJsonV2(9999, null, categoryCourseApplication.getCourseCategoryByStatus(status).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }
}
