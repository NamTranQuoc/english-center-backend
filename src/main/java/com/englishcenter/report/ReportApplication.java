package com.englishcenter.report;

import com.englishcenter.classroom.application.ClassRoomApplication;
import com.englishcenter.member.application.MemberApplication;
import com.englishcenter.report.command.CommandCountMember;
import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ReportApplication {
    @Autowired
    private MemberApplication memberApplication;
    @Autowired
    private ClassRoomApplication classRoomApplication;

    public Optional<Map<String, CommandCountMember>> countMember() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long yesterday = calendar.getTimeInMillis();
        List<BasicDBObject> aggregate = Arrays.asList(
                BasicDBObject.parse("{\"$match\": {\"type\": {\"$ne\": \"admin\"}}}"),
                BasicDBObject.parse("{\"$group\": {\"_id\": \"$type\", \"total\": {\"$sum\": 1}}}")
        );
        AggregateIterable<Document> documents = memberApplication.mongoDBConnection.aggregate(aggregate);
        Map<String, CommandCountMember> commandCountMembers = new HashMap<>();
        if (documents != null) {
            for (Document item : documents) {
                if (item.containsKey("_id") && item.get("_id") != null && StringUtils.isNotBlank(item.get("_id").toString())) {
                    commandCountMembers.put(item.getString("_id"), CommandCountMember.builder()
                            .count(item.getInteger("total"))
                            .build());
                }
            }
        }
        List<BasicDBObject> aggregate1 = Arrays.asList(
                BasicDBObject.parse("{\"$match\": {\"type\": {\"$ne\": \"admin\"}, \"create_date\": {\"$lte\": " + yesterday + "}}}"),
                BasicDBObject.parse("{\"$group\": {\"_id\": \"$type\", \"total\": {\"$sum\": 1}}}")
        );
        AggregateIterable<Document> documents1 = memberApplication.mongoDBConnection.aggregate(aggregate1);
        if (documents1 != null) {
            for (Document item : documents1) {
                if (item.containsKey("_id") && item.get("_id") != null && StringUtils.isNotBlank(item.get("_id").toString())) {
                    int count = commandCountMembers.get(item.getString("_id")).getCount();
                    int oldCount = item.getInteger("total");
                    commandCountMembers.put(item.getString("_id"), CommandCountMember.builder()
                            .count(count)
                            .percent((float) Math.floor(((float) (count - oldCount) / oldCount) * 10000) / 100)
                            .build());
                }
            }
        }
        int count = classRoomApplication.mongoDBConnection.count(new HashMap<>()).orElse(0L).intValue();
        int oldCount = classRoomApplication.mongoDBConnection.count(new Document("created_date", new Document("$lte", yesterday))).orElse(0L).intValue();
        commandCountMembers.put("classroom", CommandCountMember.builder()
                .count(count)
                .percent((float) Math.floor(((float) (count - oldCount) / oldCount) * 10000) / 100)
                .build());
        return Optional.of(commandCountMembers);
    }
}
