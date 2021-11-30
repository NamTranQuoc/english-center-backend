package com.englishcenter.core.utils.enums;

import java.util.Arrays;
import java.util.List;

public class APIOpenEnum {
    public final static List<String> apiOpen = Arrays.asList(
            "/member/add",
            "/auth/login",
            "/category_course/get_all",
            "/auth/request_forget_password/*",
            "/course/get_all",
            "/room/get_all",
            "/member/get_all",
            "/category_course/get_by_status/*",
            "/course/get_by_status/*",
            "/member/get_all_by_status",
            "/room/get_all_by_status"
    );
}
