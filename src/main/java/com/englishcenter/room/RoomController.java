package com.englishcenter.room;

import com.englishcenter.core.utils.ResponseUtils;
import com.englishcenter.core.utils.ResponseDomain;
import com.englishcenter.room.application.RoomApplication;
import com.englishcenter.room.command.CommandAddRoom;
import com.englishcenter.room.command.CommandGetAllByStatusAndCapacity;
import com.englishcenter.room.command.CommandSearchRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@Component
@RestController(value = "/room")
public class RoomController extends ResponseUtils {
    @Autowired
    private RoomApplication roomApplication;

    @GetMapping("/room/get_all")
    public ResponseDomain getAll() {
        try {
            return this.outJsonV2(9999, null, roomApplication.getAll().orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/room/get_all_by_status")
    public ResponseDomain getAllByStatus(@RequestBody CommandGetAllByStatusAndCapacity command) {
        try {
            return this.outJsonV2(9999, null, roomApplication.getAllByStatus(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/room/get_list")
    public ResponseDomain getList(@RequestBody CommandSearchRoom command, @RequestParam Integer page, @RequestParam Integer size) {
        try {
            command.setPage(page);
            command.setSize(size);
            return this.outJsonV2(9999, null, roomApplication.getList(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/room/add")
    public ResponseDomain add(@RequestBody CommandAddRoom command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            return this.outJsonV2(9999, null, roomApplication.add(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PutMapping("/room/update")
    public ResponseDomain update(@RequestBody CommandAddRoom command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            return this.outJsonV2(9999, null, roomApplication.update(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }
}
