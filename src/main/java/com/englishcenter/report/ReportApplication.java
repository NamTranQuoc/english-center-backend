package com.englishcenter.report;

import com.englishcenter.classroom.application.ClassRoomApplication;
import com.englishcenter.course.Course;
import com.englishcenter.course.application.CourseApplication;
import com.englishcenter.member.application.MemberApplication;
import com.englishcenter.report.command.CommandCountMember;
import com.englishcenter.report.command.CommandStatistical;
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
    @Autowired
    private CourseApplication courseApplication;

    public Optional<CommandStatistical> statisticalByRegister() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long yesterday = calendar.getTimeInMillis();
        List<BasicDBObject> aggregate = Arrays.asList(
                BasicDBObject.parse("{\"$unwind\": \"$student_ids\"}"),
                BasicDBObject.parse("{\"$group\": {\"_id\": \"$course_id\", \"current\": {\"$sum\": 1}, \"past\": {\"$sum\": {\"$cond\": {\"if\": {\"$lte\": [\"$student_ids.update_date\", " + yesterday + "]}, \"then\": 1, \"else\": 0 }}}}}"),
                BasicDBObject.parse("{\"$sort\": {\"current\": -1}}"),
                BasicDBObject.parse("{\"$limit\": 3}")
        );
        AggregateIterable<Document> documents = classRoomApplication.mongoDBConnection.aggregate(aggregate);
        List<CommandStatistical.Detail> details = new ArrayList<>();
        long total = 0L;
        long pastTotal = 0L;
        if (documents != null) {
            for (Document item : documents) {
                if (item.containsKey("_id") && item.get("_id") != null && StringUtils.isNotBlank(item.get("_id").toString())) {
                    long current = Long.parseLong(item.get("current").toString());
                    long past = Long.parseLong(item.get("past").toString());
                    total += current;
                    pastTotal += past;
                    Course course = courseApplication.mongoDBConnection.getById(item.getString("_id")).orElse(Course.builder()
                            .name("-")
                            .build());
                    details.add(CommandStatistical.Detail.builder()
                            .name(course.getName())
                            .total(current)
                            .build());
                }
            }
        }
        for (CommandStatistical.Detail detail : details) {
            detail.setPercent(((float) Math.floor(((float) (detail.getTotal()) / total) * 10000) / 100));
        }
        float percent = pastTotal == 0 ? 100 : ((float) Math.floor(((float) (total - pastTotal) / pastTotal) * 10000) / 100);
        return Optional.of(CommandStatistical.builder()
                .total(total)
                .percent(percent)
                .details(details)
                .build());
    }

    public Optional<CommandStatistical> statisticalByPaid() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long yesterday = calendar.getTimeInMillis();
        List<BasicDBObject> aggregate = Arrays.asList(
                BasicDBObject.parse("{\"$unwind\": \"$student_ids\"}"),
                BasicDBObject.parse("{\"$group\": {\"_id\": \"$course_id\", \"current\": {\"$sum\": \"$student_ids.amount_paid\"}, \"past\": {\"$sum\": {\"$cond\": {\"if\": {\"$lte\": [\"$student_ids.update_date\", " + yesterday + "]}, \"then\": \"$student_ids.amount_paid\", \"else\": 0}}}}}"),
                BasicDBObject.parse("{\"$sort\": {\"current\": -1}}"),
                BasicDBObject.parse("{\"$limit\": 3}")
        );
        AggregateIterable<Document> documents = classRoomApplication.mongoDBConnection.aggregate(aggregate);
        List<CommandStatistical.Detail> details = new ArrayList<>();
        long total = 0L;
        long pastTotal = 0L;
        if (documents != null) {
            for (Document item : documents) {
                if (item.containsKey("_id") && item.get("_id") != null && StringUtils.isNotBlank(item.get("_id").toString())) {
                    long current = Long.parseLong(item.get("current").toString());
                    long past = Long.parseLong(item.get("past").toString());
                    total += current;
                    pastTotal += past;
                    Course course = courseApplication.mongoDBConnection.getById(item.getString("_id")).orElse(Course.builder()
                            .name("-")
                            .build());
                    details.add(CommandStatistical.Detail.builder()
                            .name(course.getName())
                            .total(current)
                            .build());
                }
            }
        }
        for (CommandStatistical.Detail detail : details) {
            detail.setPercent(((float) Math.floor(((float) (detail.getTotal()) / total) * 10000) / 100));
        }
        float percent = pastTotal == 0 ? 100 : ((float) Math.floor(((float) (total - pastTotal) / pastTotal) * 10000) / 100);
        return Optional.of(CommandStatistical.builder()
                .total(total)
                .percent(percent)
                .details(details)
                .build());
    }

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
