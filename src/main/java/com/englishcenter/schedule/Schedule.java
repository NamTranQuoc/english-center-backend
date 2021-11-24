package com.englishcenter.schedule;

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
public class Schedule implements Serializable {
    @JsonSerialize(using = ToStringSerializer.class)
    @Id
    ObjectId _id;
    private String classroom_id;
    private String teacher_id;
    private String room_id;
    private Integer session;
    private String teaching_date;
}
