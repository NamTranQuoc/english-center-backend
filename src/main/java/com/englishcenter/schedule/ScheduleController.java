package com.englishcenter.schedule;

import com.englishcenter.core.utils.ResponseUtils;
import com.englishcenter.schedule.application.ScheduleApplication;
import com.englishcenter.schedule.command.CommandAddSchedule;
import com.englishcenter.schedule.command.CommandSearchSchedule;
import com.englishcenter.schedule.command.CommandUpdateSchedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@Component
@RestController(value = "/schedule")
public class ScheduleController extends ResponseUtils {
    @Autowired
    private ScheduleApplication scheduleApplication;

//    @GetMapping("/schedule/get_all")
//    public String getAll() {
//        try {
//            return this.outJson(9999, null, scheduleApplication.getAll().orElse(null));
//        } catch (Throwable throwable) {
//            return this.outJson(-9999, throwable.getMessage(), null);
//        }
//    }

    @PostMapping("/schedule/gets")
    public String gets(@RequestBody CommandSearchSchedule command) {
        try {
            return this.outJson(9999, null, scheduleApplication.gets(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/schedule/generate")
    public String add(@RequestBody CommandAddSchedule command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            command.setCurrent_member_id(this.getMemberId(Authorization));
            return this.outJson(9999, null, scheduleApplication.generate(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @PutMapping("/schedule/update")
    public String update(@RequestBody CommandUpdateSchedule command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            command.setCurrent_member_id(this.getMemberId(Authorization));
            return this.outJson(9999, null, scheduleApplication.update(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }
}

