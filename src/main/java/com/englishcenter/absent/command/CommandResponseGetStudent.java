package com.englishcenter.absent.command;

import com.englishcenter.member.Member;
import com.englishcenter.schedule.Schedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandResponseGetStudent {
    @Builder.Default
    private List<Member> students = new ArrayList<>();
    private Schedule schedule;
}
