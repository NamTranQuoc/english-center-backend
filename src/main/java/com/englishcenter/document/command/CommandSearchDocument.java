package com.englishcenter.document.command;

import com.englishcenter.member.command.CommandSearchMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandSearchDocument {
    private String keyword;
    private CommandSearchMember.Sort sort;
    private Integer page;
    private Integer size;
    private List<String> types;
    private List<String> course_ids;
}
