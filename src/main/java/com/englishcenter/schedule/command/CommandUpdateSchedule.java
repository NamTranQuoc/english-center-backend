package com.englishcenter.schedule.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandUpdateSchedule {
    private String id;
    private String teacher_id;
    private String room_id;
    private String role;
    private Long start_time;
    private Long end_time;
    private String current_member_id;
}
