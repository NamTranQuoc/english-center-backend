package com.englishcenter.category.course.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandAddCategoryCourse {
    private String role;
    private String id;
    private String name;
    private String status;
    private String description;
}
