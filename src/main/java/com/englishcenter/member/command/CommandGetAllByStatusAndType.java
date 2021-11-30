package com.englishcenter.member.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandGetAllByStatusAndType {
    private String type;
    private String status;
    private String course_id;
}
