package com.englishcenter.schedule.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandAddSchedule {
    private String teacher_id;
    private String classroom_id;
    private String room_id;
    private String role;
    private String current_member_id;
}
