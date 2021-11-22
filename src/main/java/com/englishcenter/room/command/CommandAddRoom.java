package com.englishcenter.room.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandAddRoom {
    private String id;
    private String name;
    private Integer capacity;
    private String status;
    private String role;
}
