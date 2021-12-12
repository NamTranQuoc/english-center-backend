package com.englishcenter.absent.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandRegisterAbsent {
    private String schedule_id;
    private String classroom_id;
    private String student_id;
}
