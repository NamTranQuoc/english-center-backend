package com.englishcenter.room.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandGetAllByStatusAndCapacity {
    private String status;
    private Integer capacity;
}
