package com.englishcenter.auth;

import com.englishcenter.auth.application.IAuthApplication;
import com.englishcenter.auth.command.CommandChangePassword;
import com.englishcenter.auth.command.CommandLogin;
import com.englishcenter.core.utils.ResponseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@Component
@RestController(value = "/auth")
public class AuthController extends ResponseUtils {
    @Autowired
    private IAuthApplication authApplication;

    @RequestMapping(value = "/auth/login", method = RequestMethod.POST)
    public String login(@RequestBody CommandLogin command) {
        try {
            return this.outJson(9999, null, authApplication.login(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @PutMapping(value = "/auth/reset")
    public String resetPassword(@RequestBody CommandChangePassword command, @RequestHeader String Authorization) {
        try {
            command.setRole(getMemberType(Authorization));
            return this.outJson(9999, null, authApplication.resetPassword(command).orElse(false));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }
}
