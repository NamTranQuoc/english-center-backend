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
public class CommandUpdateMember {
    private String id;
    private String role;
    private String name;
    private String avatar;
    private String gender;
    private String phone_number;
    private Long dob;
    private String address;
    private Long salary;
    private Member.Certificate certificate;
    private String type;
    private String current_member;
}
