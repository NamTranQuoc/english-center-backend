package com.englishcenter.auth.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandChangePassword {
    private String old_password;
    private String new_password;
    private String confirm_password;
    private String role;
    private String current_id;
    private String id;
    private String username;
}
