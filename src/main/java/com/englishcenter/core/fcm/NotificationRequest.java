package com.englishcenter.core.fcm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationRequest {

    private String target;
    private String title;
    private String body;
    @Builder.Default
    private Map<String, String> data = new HashMap<>();
}
