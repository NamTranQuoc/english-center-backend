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
    private Integer session;
    private String course_id;
    private Integer max_student;
    private Boolean took_place;
    private String classroom_id;
    private Boolean is_absent; //true : là lớp học bù
}
