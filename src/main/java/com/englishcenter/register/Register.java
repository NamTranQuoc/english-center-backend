package com.englishcenter.register;


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

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Register implements Serializable {
    @JsonSerialize(using = ToStringSerializer.class)
    @Id
    ObjectId _id;
    private String class_id;
    private String student_id;
    private String Status;
    private String update_by;

    public static class Status {
        public final static String unpaid = "unpaid";
        public final static String paid = "paid";
    }
}
