package com.englishcenter.log.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandGetRecent {
    private String perform_name;
    private String action;
    private String avatar;
    private String class_name;
    private String name;
}
