package com.englishcenter.member.application;

import com.englishcenter.core.utils.Paging;
import com.englishcenter.member.Member;
import com.englishcenter.member.command.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IMemberApplication {
    Optional<List<Member>> find(Map<String, Object> query);

    Optional<Paging<Member>> getList(CommandSearchMember command) throws Exception;

    Optional<List<CommandGetAllTeacher>> getAllByStatusAndType(CommandGetAllByStatusAndType command) throws Exception;

    Optional<String> export(CommandSearchMember commandSearchMember) throws Exception;

    Optional<Boolean> updateScoreByExcel(CommandUpdateScoreByExcel command) throws Exception;

    Optional<List<CommandGetAllTeacher>> getAll(CommandSearchMember command) throws Exception;

    Optional<Member> add(CommandAddMember command) throws Exception;

    Optional<Member> getByEmail(String email);

    Optional<Member> getByCode(String email);

    Optional<Member> getById(String id);

    Optional<Member> update(CommandUpdateMember command) throws Exception;

    Optional<Boolean> delete(String id, String role) throws Exception;
}
