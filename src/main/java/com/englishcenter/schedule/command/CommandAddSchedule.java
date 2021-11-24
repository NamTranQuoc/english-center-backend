package com.englishcenter.shift.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandAddShift {
    private String id;
    private String name;
    private String from;
    private String to;
    private String role;
}
