package com.englishcenter.log;

import com.englishcenter.core.utils.ResponseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Component
@RestController(value = "/log")
public class LogController extends ResponseUtils {
    @Autowired
    private LogApplication logApplication;

    @GetMapping("/log/get_recent")
    public String getAll() {
        try {
            return this.outJson(9999, null, logApplication.getRecent().orElse(null));
        } catch (Throwable throwable) {
            return this.outJson(-9999, throwable.getMessage(), null);
        }
    }
}
