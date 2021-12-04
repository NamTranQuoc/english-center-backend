package com.englishcenter.auth.application;

import com.englishcenter.auth.Auth;
import com.englishcenter.auth.command.CommandChangePassword;
import com.englishcenter.auth.command.CommandJwt;
import com.englishcenter.auth.command.CommandLogin;
import com.englishcenter.auth.command.CommandSignInWithGoogle;
import com.englishcenter.member.Member;

import java.util.Optional;

public interface IAuthApplication {
    Optional<Auth> add(Member member) throws Exception;

    Boolean checkJwt(String jwt);

    Optional<String> login(CommandLogin command) throws Exception;

    Optional<String> signInWithGoogle(CommandSignInWithGoogle command) throws Exception;

    Optional<CommandJwt> decodeJwt(String jwt);

    Optional<Boolean> resetPassword(CommandChangePassword command) throws Exception;

    Optional<Boolean> requestForgetPassword(String email) throws Exception;

    Optional<Boolean> forgetPassword(CommandChangePassword command) throws Exception;

    Optional<Boolean> changePassword(CommandChangePassword command) throws Exception;
}
