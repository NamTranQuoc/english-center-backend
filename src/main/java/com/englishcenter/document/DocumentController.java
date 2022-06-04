package com.englishcenter.document;

import com.englishcenter.core.utils.ResponseDomain;
import com.englishcenter.core.utils.ResponseUtils;
import com.englishcenter.document.application.DocumentApplication;
import com.englishcenter.document.command.CommandAddDocument;
import com.englishcenter.document.command.CommandSearchDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@Component
@RestController(value = "/document")
public class DocumentController extends ResponseUtils {
    @Autowired
    private DocumentApplication documentApplication;

    @GetMapping("/document/get_all")
    public ResponseDomain getAll() {
        try {
            return this.outJsonV2(9999, null, documentApplication.getAll().orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @GetMapping("/document/get_by_class")
    public ResponseDomain getByClass(@RequestParam String id, @RequestHeader String Authorization) {
        try {
            return this.outJsonV2(9999, null, documentApplication.getByClass(id, this.getMemberId(Authorization)).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @GetMapping("/document/get_advertisement")
    public ResponseDomain getImageAdvertisement() {
        try {
            return this.outJsonV2(9999, null, documentApplication.getImageAdvertisement().orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/document/get_list")
    public ResponseDomain getList(@RequestBody CommandSearchDocument command, @RequestParam Integer page, @RequestParam Integer size, @RequestHeader String Authorization) {
        try {
            command.setPage(page);
            command.setSize(size);
            command.setCurrent_member_id(this.getMemberId(Authorization));
            command.setCurrent_member_role(this.getMemberType(Authorization));
            return this.outJsonV2(9999, null, documentApplication.getList(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PostMapping("/document/add")
    public ResponseDomain add(@RequestBody CommandAddDocument command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            return this.outJsonV2(9999, null, documentApplication.add(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PutMapping("/document/update")
    public ResponseDomain update(@RequestBody CommandAddDocument command, @RequestHeader String Authorization) {
        try {
            command.setRole(this.getMemberType(Authorization));
            return this.outJsonV2(9999, null, documentApplication.update(command).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }

    @PutMapping("/document/delete/{id}")
    public ResponseDomain update(@RequestHeader String Authorization, @PathVariable String id) {
        try {
            String role = this.getMemberType(Authorization);
            return this.outJsonV2(9999, null, documentApplication.delete(id, role).orElse(null));
        } catch (Throwable throwable) {
            return this.outJsonV2(-9999, throwable.getMessage(), null);
        }
    }
}

