package com.englishcenter.core.fcm;

import lombok.Data;

@Data
public class NotificationRequest {

    private String target;
    private String title;
    private String body;
}
