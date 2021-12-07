package com.englishcenter.register;

import com.englishcenter.core.utils.Paging;
import com.englishcenter.core.utils.ResponseUtils;
import com.englishcenter.register.application.RegisterApplication;
import com.englishcenter.register.command.CommandAddRegister;
import com.englishcenter.register.command.CommandGetListRegister;
import com.englishcenter.register.command.CommandGetListResponse;
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
    public String add(@RequestBody CommandAddRegister command, @RequestHeader String Authorization) {
        try {
            command.setCurrent_member(this.getMemberId(Authorization));
            return this.outJson(9999, null, registerApplication.add(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/register/get_list")
    public String getList(@RequestBody CommandGetListRegister command, @RequestParam Integer page, @RequestParam Integer size) {
        try {
            command.setPage(page);
            command.setSize(size);
            return this.outJson(9999, null, registerApplication.getList(command).orElse(Paging.<CommandGetListResponse>builder()
                    .items(new ArrayList<>())
                    .total_items(0L)
                    .build()));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @PutMapping("/register/update")
    public String update(@RequestBody CommandAddRegister command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            command.setCurrent_member_id(this.getMemberId(Authorization));
            return this.outJson(9999, null, registerApplication.update(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @PutMapping("/register/delete")
    public String delete(@RequestBody CommandAddRegister command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            return this.outJson(9999, null, registerApplication.delete(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/register/export_excel/{id}")
    public String getList(@PathVariable String id) {
        try {
            return this.outJson(9999, null, registerApplication.exportExcel(id).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }
}
