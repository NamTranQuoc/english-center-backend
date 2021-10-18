package com.englishcenter.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/abc")
    public String get() {
        return "abc1";
    }
}
