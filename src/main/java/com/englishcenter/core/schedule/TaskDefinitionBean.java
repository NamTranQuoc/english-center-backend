package com.englishcenter.core.schedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskDefinitionBean implements Runnable {

    private TaskDefinition taskDefinition;
    @Autowired
    private TaskSchedulingService taskSchedulingService;

    @Override
    public void run() {
        System.out.println("Running action: " + taskDefinition.getActionType());
        System.out.println("With Data: " + taskDefinition.getData());
    }

    public TaskDefinition getTaskDefinition() {
        return taskDefinition;
    }

    public void setTaskDefinition(TaskDefinition taskDefinition) {
        this.taskDefinition = taskDefinition;
    }
}
