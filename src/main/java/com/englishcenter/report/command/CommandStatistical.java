package com.englishcenter.report.command;

import eu.dozd.mongo.annotation.Embedded;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandStatistical {
    private Long total;
    private Float percent;
    @Builder.Default
    private List<Detail> details = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embedded
    @Builder
    public static class Detail {
        private String name;
        private Long total;
        private Float percent;
    }
}
