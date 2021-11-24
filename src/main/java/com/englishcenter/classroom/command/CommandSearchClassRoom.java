package com.englishcenter.classroom.command;

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
public class CommandSearchClassRoom {
    private String keyword;
    private CommandSearchMember.Sort sort;
    private Integer page;
    private Integer size;
    private Long start_from_date;
    private Long start_to_date;
    private List<String> course_ids;
    private List<String> shift_ids;
    private List<Integer> dow;
}
