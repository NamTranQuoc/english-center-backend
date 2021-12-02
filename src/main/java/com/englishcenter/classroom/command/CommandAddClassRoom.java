package com.englishcenter.classroom.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandAddClassRoom {
    private String id;
    private String name;
    private Integer max_student;
    private List<Integer> dow;
    private String course_id;
    private String shift_id;
    private Long start_date;
    private String role;
    private String status;
    private String current_member_id;
}
