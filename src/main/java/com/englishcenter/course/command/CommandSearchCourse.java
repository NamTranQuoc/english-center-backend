package com.englishcenter.course.command;

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
public class CommandSearchCourse {
    private String keyword;
    private CommandSearchMember.Sort sort;
    private Long to_date;
    private Long from_date;
    private List<String> category_courses;
    private Integer page;
    private Integer size;
    private List<String> status;
}
