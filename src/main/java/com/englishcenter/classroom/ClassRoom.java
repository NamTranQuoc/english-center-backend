package com.englishcenter.classroom;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import eu.dozd.mongo.annotation.Entity;
import eu.dozd.mongo.annotation.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.io.Serializable;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassRoom implements Serializable {
    @JsonSerialize(using = ToStringSerializer.class)
    @Id
    ObjectId _id;
    private String name;
    private Integer max_student;
    private Integer min_student;
    private List<Integer> dow;
    private String course_id;
    private String shift_id;
    private Long start_date;
    private String status;
    @Builder.Default
    private Long created_date = System.currentTimeMillis();

    public static class Status {
        public final static String create = "create";
        public final static String register = "register";
    }
}
