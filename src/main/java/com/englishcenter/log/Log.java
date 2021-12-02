package com.englishcenter.log;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import eu.dozd.mongo.annotation.Embedded;
import eu.dozd.mongo.annotation.Entity;
import eu.dozd.mongo.annotation.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.util.Map;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Log {
    @JsonSerialize(using = ToStringSerializer.class)
    @Id
    ObjectId _id;
    @Builder.Default
    private Long created_date = System.currentTimeMillis();
    private String action;
    private String perform_by;
    private String class_name;
    private Map<String, ChangeDetail> detail;
    private String name;

    public static class ACTION {
        public final static String update = "update";
        public final static String add = "add";
        public final static String generate = "generate";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embedded
    @Builder
    public static class ChangeDetail {
        private String old_value;
        private String new_value;
    }
}
