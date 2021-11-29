package com.englishcenter.exam.schedule.command;

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
public class CommandSearchExamSchedule {
    private String keyword;
    private CommandSearchMember.Sort sort;
    private Integer page;
    private Integer size;
    private Long start_time;
    private Long end_time;
    private String room_id;
    private List<String> member_ids;
}
