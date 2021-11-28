package com.englishcenter.member;

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

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Member implements Serializable {
    @JsonSerialize(using = ToStringSerializer.class)
    @Id
    ObjectId _id;
    private String name;
    private Long create_date;
    private String type;
    private String email;
    private String avatar;
    private Long dob;
    private String address;
    private String phone_number;
    private String gender;
    private Long salary;
    private Certificate certificate;
    @Builder.Default
    private Boolean is_deleted = false;
    @Builder.Default
    private Score input_score = Score.builder().build();
    @Builder.Default
    private Score current_score = Score.builder().build();


    public static class MemberType {
        public final static String ADMIN = "admin";
        public final static String STUDENT = "student";
        public final static String TEACHER = "teacher";
        public final static String RECEPTIONIST = "receptionist";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embedded
    @Builder
    public static class Certificate {
        private String type;
        private Float score;
        private String code;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embedded
    @Builder
    public static class Score {
        @Builder.Default
        private Float read = 0f;
        @Builder.Default
        private Float listen = 0f;
        @Builder.Default
        private Float total = 0f;
    }
}
