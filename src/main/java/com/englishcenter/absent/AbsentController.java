package com.englishcenter.absent;

import com.englishcenter.absent.command.CommandGetAbsent;
import com.englishcenter.absent.command.CommandGetStudent;
import com.englishcenter.absent.command.CommandMuster;
import com.englishcenter.absent.command.CommandRegisterAbsent;
import com.englishcenter.core.utils.ResponseDomain;
import com.englishcenter.core.utils.ResponseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Component
@RestController(value = "/absent")
public class AbsentController extends ResponseUtils {
    @Autowired
    private AbsentApplication absentApplication;

    @PostMapping("/absent/get_students")
    public ResponseDomain getList(@RequestBody CommandGetStudent command, @RequestHeader String Authorization) {
        try {
            command.setCurrent_member_id(getMemberId(Authorization));
            return this.outJsonV2(9999, null, absentApplication.getStudents(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/absent/save_absent")
    public ResponseDomain save(@RequestBody CommandMuster command, @RequestHeader String Authorization) {
        try {
            return this.outJsonV2(9999, null, absentApplication.saveAbsent(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/absent/get_classroom_absents")
    public ResponseDomain getClassroomAbsents(@RequestBody CommandGetAbsent command, @RequestHeader String Authorization) {
        try {
            command.setStudent_id(getMemberId(Authorization));
            return this.outJsonV2(9999, null, absentApplication.getClassroomAbsents(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/absent/register_absent")
    public ResponseDomain registerAbsent(@RequestBody CommandRegisterAbsent command, @RequestHeader String Authorization) {
        try {
            command.setStudent_id(getMemberId(Authorization));
            return this.outJsonV2(9999, null, absentApplication.registerAbsent(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }
}
