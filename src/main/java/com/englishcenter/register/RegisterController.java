package com.englishcenter.register;

import com.englishcenter.classroom.command.CommandAddClassRoom;
import com.englishcenter.core.utils.ResponseUtils;
import com.englishcenter.member.application.IMemberApplication;
import com.englishcenter.register.application.RegisterApplication;
import com.englishcenter.register.command.CommandAddRegister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Component
@RestController(value = "/register")
public class RegisterController extends ResponseUtils {
    @Autowired
    private RegisterApplication registerApplication;

    @PostMapping("/register/add")
    public String add(@RequestBody CommandAddRegister command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            return this.outJson(9999, null, registerApplication.add(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }
}
