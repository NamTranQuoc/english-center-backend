package com.englishcenter.course;

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
public class Course implements Serializable {
    @JsonSerialize(using = ToStringSerializer.class)
    @Id
    ObjectId _id;
    private String name;
    private Long tuition;
    private Integer number_of_shift;
    private String description;
    private String category_course_id;
    private Long create_date;
}
