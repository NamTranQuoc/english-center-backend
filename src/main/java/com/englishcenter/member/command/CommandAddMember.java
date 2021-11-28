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
public class CommandAddMember {
    private String name;
    private String email;
    private String type;
    private Long dob;
    private String address;
    private String phone_number;
    private String gender;
    private String nick_name;
    private String note;
    private Member.Guardian guardian;
    private List<String> course_ids;
    private String status;
}

