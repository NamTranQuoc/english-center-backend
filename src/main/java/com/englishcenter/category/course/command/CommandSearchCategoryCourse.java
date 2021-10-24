package com.englishcenter.category.course.command;

import com.englishcenter.member.command.CommandSearchMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandSearchCategoryCourse {
    private String keyword;
    private CommandSearchMember.Sort sort;
    private Long to_date;
    private Long from_date;
    private Integer page;
    private Integer size;
}
