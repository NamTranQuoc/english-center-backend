package com.englishcenter.category.course.command;

import eu.dozd.mongo.annotation.Embedded;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandGetAllResponse {
    private String name;
    private List<Course> courses;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embedded
    @Builder
    public static class Course {
        private String name;
        private String id;
        private Long tuition;
        private Integer number_of_shift;
        private Float input_score;
        private Float output_score;
    }
}
