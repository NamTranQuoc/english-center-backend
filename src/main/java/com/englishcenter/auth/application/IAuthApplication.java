package com.englishcenter.auth.application;

import com.englishcenter.auth.Auth;
import com.englishcenter.auth.command.CommandJwt;
import com.englishcenter.auth.command.CommandLogin;
import com.englishcenter.member.Member;

import java.util.Optional;

public interface IAuthApplication {
    Optional<Auth> add(Member member) throws Exception;

    Boolean checkJwt(String jwt);

    Optional<String> login(CommandLogin command) throws Exception;

    Optional<CommandJwt> decodeJwt(String jwt);
}
