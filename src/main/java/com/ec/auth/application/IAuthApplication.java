package com.ec.auth.application;

import com.ec.auth.Auth;
import com.ec.auth.command.CommandJwt;
import com.ec.auth.command.CommandLogin;
import com.ec.member.Member;

import java.util.Optional;

public interface IAuthApplication {
    Optional<Auth> add(Member member, String password) throws Exception;

    Optional<Auth> checkJwt(String jwt) throws Exception;

    Optional<String> login(CommandLogin command) throws Exception;

    Optional<CommandJwt> decodeJwt(String jwt);
}
