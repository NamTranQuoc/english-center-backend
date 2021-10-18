package com.ec.member.application;

import com.ec.member.Member;
import com.ec.member.command.CommandAddMember;
import com.ec.member.command.CommandSearchMember;
import com.ec.member.command.CommandUpdateMember;
import com.utils.Paging;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IMemberApplication {
    Optional<List<Member>> find(Map<String, Object> query);

    Optional<Paging<Member>> getList(CommandSearchMember command) throws Exception;

    Optional<Member> add(CommandAddMember command) throws Exception;

    Optional<Member> getById(String id);

    Optional<Member> update(CommandUpdateMember command) throws Exception;
}
