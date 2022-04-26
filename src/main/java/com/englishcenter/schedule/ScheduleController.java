package com.englishcenter.schedule;

import com.englishcenter.core.mail.Mail;
import com.englishcenter.core.utils.ResponseDomain;
import com.englishcenter.core.utils.ResponseUtils;
import com.englishcenter.schedule.application.ScheduleApplication;
import com.englishcenter.schedule.command.CommandAddSchedule;
import com.englishcenter.schedule.command.CommandSearchSchedule;
import com.englishcenter.schedule.command.CommandUpdateSchedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@Component
@RestController(value = "/schedule")
public class ScheduleController extends ResponseUtils {
    @Autowired
    private ScheduleApplication scheduleApplication;

    //    @GetMapping("/schedule/get_all")
//    public ResponseDomain getAll() {
//        try {
//            return this.outJsonV2(9999, null, scheduleApplication.getAll().orElse(null));
//        } catch (Throwable throwable) {
//            return this.outJsonV2(-9999, throwable.getMessage(), null);
//        }
//    }
    @Autowired
    private KafkaTemplate<String, Mail> mailKafkaTemplate;

    @PostMapping("/schedule/gets")
    public ResponseDomain gets(@RequestBody CommandSearchSchedule command, @RequestHeader String Authorization) {
        try {
            command.setCurrent_member_role(this.getMemberType(Authorization));
            command.setCurrent_member_id(this.getMemberId(Authorization));
            return this.outJsonV2(9999, null, scheduleApplication.gets(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/schedule/gets_v2")
    public ResponseDomain getsV2(@RequestBody CommandSearchSchedule command, @RequestHeader String Authorization) {
        try {
            command.setCurrent_member_role(this.getMemberType(Authorization));
            command.setCurrent_member_id(this.getMemberId(Authorization));
            return this.outJsonV2(9999, null, scheduleApplication.getsV2(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/schedule/generate")
    public ResponseDomain add(@RequestBody CommandAddSchedule command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            command.setCurrent_member_id(this.getMemberId(Authorization));
            return this.outJsonV2(9999, null, scheduleApplication.generate(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PutMapping("/schedule/update")
    public ResponseDomain update(@RequestBody CommandUpdateSchedule command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            command.setCurrent_member_id(this.getMemberId(Authorization));
            return this.outJsonV2(9999, null, scheduleApplication.update(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }
}

