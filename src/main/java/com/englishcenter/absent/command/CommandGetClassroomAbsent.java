package com.englishcenter.absent.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandGetClassroomAbsent {
    private String _id;
    private String name;
}
