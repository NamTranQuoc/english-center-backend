package com.englishcenter.core.schedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/schedule")
public class JobSchedulingController {

    @Autowired
    private TaskSchedulingService taskSchedulingService;

    @Autowired
    private TaskDefinitionBean taskDefinitionBean;

    @PostMapping(path = "/taskdef", consumes = "application/json", produces = "application/json")
    public void scheduleATask(@RequestBody TaskDefinition taskDefinition) {
        taskDefinitionBean.setTaskDefinition(taskDefinition);
        taskSchedulingService.scheduleATask(taskDefinitionBean, taskDefinition.getStartTime(), ScheduleName.SCHEDULE_REMIND, "fasdfasdfasdfa283feihud");
    }

    @GetMapping(path = "/remove/{name}/{ref_id}")
    public void removeJob(@PathVariable String name, @PathVariable String ref_id) {
        taskSchedulingService.removeScheduledTask(name, ref_id);
    }
}
