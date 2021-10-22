package com.englishcenter.member.command;

import com.englishcenter.member.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandAddMember {
    private String name;
    private String email;
    private String type;
    private String avatar;
    private Long dob;
    private String address;
    private String phone_number;
    private String gender;
    private Long salary;
    private Member.Certificate certificate;
}

