package com.englishcenter.core.utils.enums;

import java.util.Arrays;
import java.util.List;

public class APIOpenEnum {
    public final static List<String> apiOpen = Arrays.asList(
            "/member/add",
            "/auth/login",
            "/category_course/get_all",
            "/auth/request_forget_password/*"
    );
}
