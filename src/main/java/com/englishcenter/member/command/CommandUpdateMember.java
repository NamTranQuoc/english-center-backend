package com.englishcenter.member.command;

import com.englishcenter.member.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandUpdateMember {
    private String id;
    private String role;
    private String name;
    private String gender;
    private String phone_number;
    private Long dob;
    private String address;
    private String type;
    private String current_member;
    private String nick_name;
    private String note;
    private Member.Guardian guardian;
    private List<String> course_ids;
    private String status;
}
