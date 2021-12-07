package com.englishcenter.member.command;

import eu.dozd.mongo.annotation.Embedded;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandSearchMember {
    private String member_type;
    private Integer size;
    private Integer page;
    private String keyword;
    private List<String> types;
    private Long from_date;
    private Long to_date;
    private Long from_dob_date;
    private Long to_dob_date;
    private Sort sort;
    private List<String> genders;
    private List<String> course_ids;
    private Float min_input_score;
    private Float max_input_score;
    private Float min_output_score;
    private Float max_output_score;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embedded
    @Builder
    public static class Sort {
        private String field;
        private Boolean is_asc;
    }
}
