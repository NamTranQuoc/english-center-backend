package com.englishcenter.mytest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyTestController {
    @GetMapping("/abc")
    public String get() {
        return "abc";
    }
}
