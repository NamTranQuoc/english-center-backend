package com.englishcenter.register.command;

import com.englishcenter.classroom.ClassRoom;
import com.englishcenter.member.Member;
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
    private ClassRoom.StudentRegister register;
    private String class_id;
}
