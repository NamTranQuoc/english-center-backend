package com.englishcenter.course.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandAddCourse {
    private String role;
    private String id;
    private String name;
    private Long tuition;
    private Integer number_of_shift;
    private String description;
    private String category_course_id;
    private Float input_score;
    private Float output_score;
    private String status;
    private String current_member_id;
}
