package com.englishcenter.absent;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import eu.dozd.mongo.annotation.Entity;
import eu.dozd.mongo.annotation.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Absent {
    @JsonSerialize(using = ToStringSerializer.class)
    @Id
    ObjectId _id;

    private String student_id;
    private String classroom_id;
    private Integer session;
    private String backup_classroom_id;
    private String schedule_id;
    private String backup_schedule_id;
}
