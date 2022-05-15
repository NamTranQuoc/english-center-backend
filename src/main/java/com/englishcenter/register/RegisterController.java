package com.englishcenter.register;

import com.englishcenter.core.utils.Paging;
import com.englishcenter.core.utils.ResponseDomain;
import com.englishcenter.core.utils.ResponseUtils;
import com.englishcenter.register.application.RegisterApplication;
import com.englishcenter.register.command.CommandAddRegister;
import com.englishcenter.register.command.CommandGetListRegister;
import com.englishcenter.register.command.CommandGetListResponse;
import com.englishcenter.register.command.CommandGetsByClassIdAndSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@Component
@RestController(value = "/register")
public class RegisterController extends ResponseUtils {
    @Autowired
    private RegisterApplication registerApplication;

    @PostMapping("/register/add")
    public ResponseDomain add(@RequestBody CommandAddRegister command, @RequestHeader String Authorization) {
        try {
            command.setCurrent_member(this.getMemberId(Authorization));
            return this.outJsonV2(9999, null, registerApplication.add(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/register/get_list")
    public ResponseDomain getList(@RequestBody CommandGetListRegister command, @RequestParam Integer page, @RequestParam Integer size) {
        try {
            command.setPage(page);
            command.setSize(size);
            return this.outJsonV2(9999, null, registerApplication.getList(command).orElse(Paging.<CommandGetListResponse>builder()
                    .items(new ArrayList<>())
                    .total_items(0L)
                    .build()));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/register/gets_by_class")
    public ResponseDomain getList(@RequestBody CommandGetsByClassIdAndSession command, @RequestHeader String Authorization) {
        try {
            command.setRole(getMemberType(Authorization));
            return this.outJsonV2(9999, null, registerApplication.getsByClassroomId(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @GetMapping("/register/gets_by_student")
    public ResponseDomain getListByStudent(@RequestHeader String Authorization) {
        try {
            return this.outJsonV2(9999, null, registerApplication.getsByStudent(getMemberId(Authorization)).orElse(new ArrayList<>()));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PutMapping("/register/update")
    public ResponseDomain update(@RequestBody CommandAddRegister command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            command.setCurrent_member_id(this.getMemberId(Authorization));
            return this.outJsonV2(9999, null, registerApplication.update(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PutMapping("/register/delete")
    public ResponseDomain delete(@RequestBody CommandAddRegister command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            return this.outJsonV2(9999, null, registerApplication.delete(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/register/export_excel/{id}")
    public ResponseDomain getList(@PathVariable String id) {
        try {
            return this.outJsonV2(9999, null, registerApplication.exportExcel(id).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/register/add_v2")
    public ResponseDomain addV2(@RequestBody CommandAddRegister command, @RequestHeader String Authorization) {
        try {
            command.setCurrent_member(this.getMemberId(Authorization));
            command.setStudent_id(this.getMemberId(Authorization));
            return this.outJsonV2(9999, null, registerApplication.add(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }
}
