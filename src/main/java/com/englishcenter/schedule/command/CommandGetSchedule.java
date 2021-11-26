package com.englishcenter.schedule.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandGetSchedule {
    private String title;
    private String id;
    private String teacher_id;
    private String room_id;
    private Long start;
    private Long end;
}
