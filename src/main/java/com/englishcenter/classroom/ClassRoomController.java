package com.englishcenter.classroom;

import com.englishcenter.classroom.application.ClassRoomApplication;
import com.englishcenter.classroom.command.CommandAddClassRoom;
import com.englishcenter.classroom.command.CommandSearchClassRoom;
import com.englishcenter.core.utils.ResponseDomain;
import com.englishcenter.core.utils.ResponseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@Component
@RestController(value = "/class")
public class ClassRoomController extends ResponseUtils {
    @Autowired
    private ClassRoomApplication classRoomApplication;

    @GetMapping("/class/get_all")
    public ResponseDomain getAll() {
        try {
            return this.outJsonV2(9999, null, classRoomApplication.getAll().orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @GetMapping("/class/get_by_keyword")
    public ResponseDomain getByKeyWord(@RequestParam String keyword) {
        try {
            return this.outJsonV2(9999, null, classRoomApplication.getByKeyWord(keyword).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/class/get_list")
    public ResponseDomain getList(@RequestBody CommandSearchClassRoom command, @RequestParam Integer page, @RequestParam Integer size) {
        try {
            command.setPage(page);
            command.setSize(size);
            return this.outJsonV2(9999, null, classRoomApplication.getList(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/class/add")
    public ResponseDomain add(@RequestBody CommandAddClassRoom command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            command.setCurrent_member_id(this.getMemberId(Authorization));
            return this.outJsonV2(9999, null, classRoomApplication.add(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PutMapping("/class/update")
    public ResponseDomain update(@RequestBody CommandAddClassRoom command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            command.setCurrent_member_id(this.getMemberId(Authorization));
            return this.outJsonV2(9999, null, classRoomApplication.update(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @GetMapping("/class/get_by_course_id/{id}")
    public ResponseDomain getByCourseId(@PathVariable String id) {
        try {
            return this.outJsonV2(9999, null, classRoomApplication.getClassByCourseId(id).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }
}
