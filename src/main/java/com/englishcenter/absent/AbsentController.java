package com.englishcenter.absent;

import com.englishcenter.absent.command.CommandGetStudent;
import com.englishcenter.absent.command.CommandMuster;
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
    public String getList(@RequestBody CommandGetStudent command, @RequestHeader String Authorization) {
        try {
            command.setCurrent_member_id(getMemberId(Authorization));
            return this.outJson(9999, null, absentApplication.getStudents(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/absent/save_absent")
    public String save(@RequestBody CommandMuster command, @RequestHeader String Authorization) {
        try {
            return this.outJson(9999, null, absentApplication.saveAbsent(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }
}
