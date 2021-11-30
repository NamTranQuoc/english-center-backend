package com.englishcenter.exam.schedule;

import com.englishcenter.classroom.application.ClassRoomApplication;
import com.englishcenter.classroom.command.CommandAddClassRoom;
import com.englishcenter.classroom.command.CommandSearchClassRoom;
import com.englishcenter.core.utils.ResponseUtils;
import com.englishcenter.exam.schedule.application.ExamScheduleApplication;
import com.englishcenter.exam.schedule.command.CommandAddExamSchedule;
import com.englishcenter.exam.schedule.command.CommandSearchExamSchedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@Component
@RestController(value = "/exam_schedule")
public class ExamScheduleController extends ResponseUtils  {
    @Autowired
    private ExamScheduleApplication examScheduleApplication;

    @GetMapping("/exam_schedule/get_all")
    public String getAll() {
        try {
            return this.outJson(9999, null, examScheduleApplication.getAll().orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/exam_schedule/get_list")
    public String getList(@RequestBody CommandSearchExamSchedule command, @RequestParam Integer page, @RequestParam Integer size) {
        try {
            command.setPage(page);
            command.setSize(size);
            return this.outJson(9999, null, examScheduleApplication.getList(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/exam_schedule/add")
    public String add(@RequestBody CommandAddExamSchedule command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            return this.outJson(9999, null, examScheduleApplication.add(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @PutMapping("/exam_schedule/update")
    public String update(@RequestBody CommandAddExamSchedule command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            return this.outJson(9999, null, examScheduleApplication.update(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }
}
