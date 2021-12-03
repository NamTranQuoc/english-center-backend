package com.englishcenter.register.command;

import com.englishcenter.member.Member;
import com.englishcenter.register.Register;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandGetListResponse {
    private Member member;
    private Register.StudentRegister register;
    private String class_id;
}
