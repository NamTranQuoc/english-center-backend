package com.englishcenter.document.command;

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
public class CommandAddDocument {
    private String id;
    private String name;
    private String type;
    private String path;
    @Builder.Default
    private List<String> course_ids = new ArrayList<>();
    private String role;
}
