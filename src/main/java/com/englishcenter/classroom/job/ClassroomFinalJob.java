package com.englishcenter.classroom.job;

import com.englishcenter.classroom.ClassRoom;
import com.englishcenter.classroom.application.ClassRoomApplication;
import com.englishcenter.core.schedule.ScheduleName;
import com.englishcenter.core.schedule.TaskSchedulingService;
import lombok.Data;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Data
public class ClassroomFinalJob implements Runnable {
    private String classroomId;
    private TaskSchedulingService taskSchedulingService;

    @SneakyThrows
    @Override
    public void run() {
        ClassRoomApplication classRoomApplication = new ClassRoomApplication();

        taskSchedulingService.cleanJobWhenRun(ScheduleName.CLASSROOM_FINAL, classroomId);

        //handle
        Optional<ClassRoom> optional = classRoomApplication.getById(classroomId);

        if (!optional.isPresent()) {
            return;
        }

        ClassRoom classRoom = optional.get();

        classRoom.setStatus(ClassRoom.Status.finish);
        classRoomApplication.mongoDBConnection.update(classroomId, classRoom);
    }
}
