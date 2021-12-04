package com.englishcenter.auth;

import com.englishcenter.auth.application.IAuthApplication;
import com.englishcenter.auth.command.CommandChangePassword;
import com.englishcenter.auth.command.CommandLogin;
import com.englishcenter.auth.command.CommandSignInWithGoogle;
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

    @PostMapping(value = "/auth/sign_with_google")
    public String SignInWithGoogle(@RequestBody CommandSignInWithGoogle command) {
        try {
            return this.outJson(9999, null, authApplication.signInWithGoogle(command).orElse(null));
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

    @GetMapping(value = "/auth/request_forget_password/{email}")
    public String requestForgetPassword(@PathVariable String email) {
        try {
            return this.outJson(9999, null, authApplication.requestForgetPassword(email).orElse(false));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping(value = "/auth/forget_password")
    public String forgetPassword(@RequestHeader String Authorization, @RequestBody CommandChangePassword command) {
        try {
            command.setCurrent_id(this.getMemberId(Authorization));
            return this.outJson(9999, null, authApplication.forgetPassword(command).orElse(false));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping(value = "/auth/change_password")
    public String changePassword(@RequestHeader String Authorization, @RequestBody CommandChangePassword command) {
        try {
            command.setCurrent_id(this.getMemberId(Authorization));
            return this.outJson(9999, null, authApplication.changePassword(command).orElse(false));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }
}
