package com.ec.auth;

import com.ec.auth.application.IAuthApplication;
import com.ec.auth.command.CommandLogin;
import com.utils.ResponseUtils;
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
}
