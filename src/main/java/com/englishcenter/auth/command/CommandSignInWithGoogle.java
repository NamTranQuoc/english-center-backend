package com.englishcenter.auth.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandSignInWithGoogle {
    private String email;
    private String avatar;
    private String phone_number;
    private String name;
}
