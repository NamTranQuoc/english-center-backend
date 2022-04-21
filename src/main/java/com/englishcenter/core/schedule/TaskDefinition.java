package com.englishcenter.core.schedule;

import lombok.Data;

@Data
public class TaskDefinition {
    private Long startTime;
    private String actionType;
    private String data;
}
