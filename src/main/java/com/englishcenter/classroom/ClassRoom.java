package com.englishcenter.classroom;

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

import java.io.Serializable;
import java.util.ArrayList;
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
    @Builder.Default
    private List<StudentRegister> student_ids = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embedded
    @Builder
    public static class StudentRegister {
        private String student_id;
        private String status;
        private String update_by;
        private Long update_date;
        @Builder.Default
        private Long amount_paid = 0L;
    }

    public static class Status {
        public final static String create = "create";
        public final static String register = "register";
        public final static String coming = "coming";
        public final static String cancel = "cancel";
        public final static String finish = "finish";
    }

    public static class RegisterStatus {
        public final static String unpaid = "unpaid";
        public final static String paid = "paid";
    }
}
