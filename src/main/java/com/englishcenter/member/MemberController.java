package com.englishcenter.member;

import com.englishcenter.core.utils.ResponseUtils;
import com.englishcenter.member.application.IMemberApplication;
import com.englishcenter.member.command.CommandAddMember;
import com.englishcenter.member.command.CommandSearchMember;
import com.englishcenter.member.command.CommandUpdateMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@Component
@RestController(value = "/member")
public class MemberController extends ResponseUtils {
    @Autowired
    private IMemberApplication userApplication;

    @RequestMapping(value = "/member/get_all", method = RequestMethod.GET)
    public String get() {
        try {
            return "success";
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping(value = "/member/get_list")
    public String getList(@RequestBody CommandSearchMember command, @RequestParam Integer page, @RequestParam Integer size, @RequestHeader String Authorization) {
        try {
            command.setPage(page);
            command.setSize(size);
            command.setMember_type(this.getMemberType(Authorization));
            return this.outJson(9999, null, userApplication.getList(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @RequestMapping(value = "/member/add", method = RequestMethod.POST)
    public String add(@RequestBody CommandAddMember command) {
        try {
            return this.outJson(9999, null, userApplication.add(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @PutMapping(value = "/member/update")
    public String update(@RequestBody CommandUpdateMember command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            return this.outJson(9999, null, userApplication.update(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @DeleteMapping(value = "/member/delete/{id}")
    public String delete(@RequestHeader String Authorization, @PathVariable String id) {
        try {
            String role = this.getMemberType(Authorization);
            return this.outJson(9999, null, userApplication.delete(id, role).orElse(false));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }

    @GetMapping(value = "/member/get_current")
    public String getById(@RequestHeader String Authorization) {
        try {
            String currentId = this.getMemberId(Authorization);
            return this.outJson(9999, null, userApplication.getById(currentId).orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }
}
