package com.englishcenter.exam.schedule.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandAddExamSchedule {
    private String id;
    private Long start_time;
    private Long end_time;
    private String room_id;
    @Builder.Default
    private List<String> member_ids = new ArrayList<>();
    private Integer max_quantity;
    private Integer min_quantity;
    private String role;
}
