package com.englishcenter.register.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandAddRegister {
    private String class_id;
    private String student_id;
    private String Status;
    private String update_by;
    private String current_member;
    private String role;
    private String Current_member_id;
}
