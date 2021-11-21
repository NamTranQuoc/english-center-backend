package com.englishcenter.shift;

import com.englishcenter.category.course.application.ICategoryCourseApplication;
import com.englishcenter.category.course.command.CommandAddCategoryCourse;
import com.englishcenter.category.course.command.CommandSearchCategoryCourse;
import com.englishcenter.core.utils.ResponseUtils;
import com.englishcenter.shift.application.ShiftApplication;
import com.englishcenter.shift.command.CommandAddShift;
import com.englishcenter.shift.command.CommandSearchShift;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@Component
@RestController(value = "/shift")
public class ShiftController extends ResponseUtils {
    @Autowired
    private ShiftApplication shiftApplication;

    @GetMapping("/shift/get_all")
    public String getAll() {
        try {
            return this.outJson(9999, null, shiftApplication.getAll().orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/shift/get_list")
    public String getList(@RequestBody CommandSearchShift command, @RequestParam Integer page, @RequestParam Integer size) {
        try {
            command.setPage(page);
            command.setSize(size);
            return this.outJson(9999, null, shiftApplication.getList(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/shift/add")
    public String add(@RequestBody CommandAddShift command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            return this.outJson(9999, null, shiftApplication.add(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @PutMapping("/shift/update")
    public String update(@RequestBody CommandAddShift command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            return this.outJson(9999, null, shiftApplication.update(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }
}

