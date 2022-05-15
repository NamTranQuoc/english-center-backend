package com.englishcenter.core.fcm;

import lombok.Data;

import java.util.List;

@Data
public class SubscriptionRequest {

    String topicName;
    List<String> tokens;
}
