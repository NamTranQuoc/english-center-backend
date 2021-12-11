package com.englishcenter.register.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandGetsByClassIdAndSession {
    private String classroom_id;
    private String keyword;
    private Integer session;
    private String role;
}
