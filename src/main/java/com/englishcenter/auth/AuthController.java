package com.englishcenter.auth;

import com.englishcenter.auth.application.IAuthApplication;
import com.englishcenter.auth.command.CommandChangePassword;
import com.englishcenter.auth.command.CommandLogin;
import com.englishcenter.auth.command.CommandSignInWithGoogle;
import com.englishcenter.core.utils.ResponseDomain;
import com.englishcenter.core.utils.ResponseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@Component
@RestController(value = "/auth")
public class AuthController extends ResponseUtils {
    @Autowired
    private IAuthApplication authApplication;

    @ResponseBody
    @RequestMapping(value = "/auth/login", method = RequestMethod.POST)
    public ResponseDomain login(@RequestBody CommandLogin command) {
        try {
            return this.outJsonV2(9999, null, authApplication.login(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping(value = "/auth/sign_with_google")
    public ResponseDomain SignInWithGoogle(@RequestBody CommandSignInWithGoogle command) {
        try {
            return this.outJsonV2(9999, null, authApplication.signInWithGoogle(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PutMapping(value = "/auth/reset")
    public ResponseDomain resetPassword(@RequestBody CommandChangePassword command, @RequestHeader String Authorization) {
        try {
            command.setRole(getMemberType(Authorization));
            return this.outJsonV2(9999, null, authApplication.resetPassword(command).orElse(false));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @GetMapping(value = "/auth/request_forget_password/{email}")
    public ResponseDomain requestForgetPassword(@PathVariable String email) {
        try {
            return this.outJsonV2(9999, null, authApplication.requestForgetPassword(email).orElse(false));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @GetMapping(value = "/auth/request_code_forget_password/{email}")
    public ResponseDomain requestCodeForgetPassword(@PathVariable String email) {
        try {
            return this.outJsonV2(9999, null, authApplication.requestCodeForgetPassword(email).orElse(false));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping(value = "/auth/forget_password")
    public ResponseDomain forgetPassword(@RequestHeader String Authorization, @RequestBody CommandChangePassword command) {
        try {
            command.setCurrent_id(this.getMemberId(Authorization));
            return this.outJsonV2(9999, null, authApplication.forgetPassword(command).orElse(false));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping(value = "/auth/forget_password/code")
    public ResponseDomain forgetPassword(@RequestBody CommandChangePassword command) {
        try {
            return this.outJsonV2(9999, null, authApplication.forgetPasswordCode(command).orElse(false));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping(value = "/auth/change_password")
    public ResponseDomain changePassword(@RequestHeader String Authorization, @RequestBody CommandChangePassword command) {
        try {
            command.setCurrent_id(this.getMemberId(Authorization));
            return this.outJsonV2(9999, null, authApplication.changePassword(command).orElse(false));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }
}
