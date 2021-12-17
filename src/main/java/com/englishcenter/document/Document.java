package com.englishcenter.document;

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
public class Document implements Serializable {
    @JsonSerialize(using = ToStringSerializer.class)
    @Id
    ObjectId _id;
    private String name;
    private String code;
    private String type;
    private String path;
    private List<String> course_ids;
    @Builder.Default
    private Boolean is_deleted = false;

    public static class DocumentType {
        public final static String IMAGE = "image";
        public final static String DOC = "doc";
        public final static String ADVERTISEMENT = "advertisement";
    }
}
