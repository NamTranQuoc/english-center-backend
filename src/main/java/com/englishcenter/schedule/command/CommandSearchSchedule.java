package com.englishcenter.schedule.command;

import com.englishcenter.member.command.CommandSearchMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandSearchSchedule {
    private String keyword;
    private CommandSearchMember.Sort sort;
    private Integer page;
    private Integer size;
}
