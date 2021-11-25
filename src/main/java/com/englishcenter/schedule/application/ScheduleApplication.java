package com.englishcenter.schedule.application;

import com.englishcenter.classroom.ClassRoom;
import com.englishcenter.classroom.application.ClassRoomApplication;
import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.Paging;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.course.Course;
import com.englishcenter.course.application.CourseApplication;
import com.englishcenter.member.Member;
import com.englishcenter.member.application.MemberApplication;
import com.englishcenter.room.Room;
import com.englishcenter.room.application.RoomApplication;
import com.englishcenter.schedule.Schedule;
import com.englishcenter.schedule.command.CommandAddSchedule;
import com.englishcenter.schedule.command.CommandSearchSchedule;
import com.englishcenter.shift.Shift;
import com.englishcenter.shift.application.ShiftApplication;
import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class ScheduleApplication {
    public final MongoDBConnection<Schedule> mongoDBConnection;

    @Autowired
    public ScheduleApplication() {
        mongoDBConnection = new MongoDBConnection<>(MongodbEnum.collection_schedule, Schedule.class);
    }
    @Autowired
    private ClassRoomApplication classRoomApplication;
    @Autowired
    private MemberApplication memberApplication;
    @Autowired
    private RoomApplication roomApplication;
    @Autowired
    private CourseApplication courseApplication;
    @Autowired
    private ShiftApplication shiftApplication;

    public Optional<Schedule> add(CommandAddSchedule command) throws Exception {

        return Optional.empty();
    }

    public Optional<Boolean> generate(CommandAddSchedule command) throws Exception {
        if(StringUtils.isAnyBlank(command.getClassroom_id(), command.getRole())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        Optional<ClassRoom> classRoomOptional = classRoomApplication.getById(command.getClassroom_id());
        if (!classRoomOptional.isPresent()) {
            throw new Exception(ExceptionEnum.classroom_not_exist);
        }
        ClassRoom classRoom = classRoomOptional.get();
        Optional<Course> optionalCourse = courseApplication.getById(classRoom.getCourse_id());
        if (!optionalCourse.isPresent()) {
            throw new Exception(ExceptionEnum.course_not_exist);
        }
        Optional<Shift> optionalShift = shiftApplication.getById(classRoom.getShift_id());
        if (!optionalShift.isPresent()) {
            throw new Exception(ExceptionEnum.shift_not_exist);
        }
        Shift shift = optionalShift.get();
        Course course = optionalCourse.get();
        validateScheduleExits(classRoom.get_id().toHexString());
        List<String> listTeacher = getTeacherIds(command.getTeacher_id());
        List<String> listRoom = getRoomIds(command.getRoom_id());
        List<Schedule> schedule = new ArrayList<>();
        if (CollectionUtils.isEmpty(listRoom)) {
            throw new Exception(ExceptionEnum.room_not_exist);
        }
        if (CollectionUtils.isEmpty((listTeacher))) {
            throw new Exception(ExceptionEnum.member_not_exist);
        }

        Date time = new Timestamp(classRoom.getStart_date());
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = dateFormat.format(time);
        long start = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(strDate + " " + shift.getFrom()).getTime();
        long end = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(strDate + " " + shift.getTo()).getTime();
        Calendar cStart = GregorianCalendar.getInstance();
        cStart.setTimeInMillis(start);
        Calendar cEnd = GregorianCalendar.getInstance();
        cEnd.setTimeInMillis(end);
        int i = 1;
        while (i <= course.getNumber_of_shift()) {
            int dayOfWeek = cStart.get(Calendar.DAY_OF_WEEK) + 1;
            if (classRoom.getDow().contains(dayOfWeek)) {
                String roomId = null;
                Map<String, Object> query = new HashMap<>();
                query.put("$or", Arrays.asList(
                        new Document("$and", Arrays.asList(
                                new Document("start_date", new Document("$lte", cStart.getTimeInMillis())),
                                new Document("end_date", new Document("$gte", cStart.getTimeInMillis()))
                        )),
                        new Document("$and", Arrays.asList(
                                new Document("start_date", new Document("$lte", cEnd.getTimeInMillis())),
                                new Document("end_date", new Document("$gte", cEnd.getTimeInMillis()))
                        ))
                ));
                for (String item: listRoom) {
                    query.put("room_id", item);
                    long count = mongoDBConnection.count(query).orElse(0L);
                    if (count == 0) {
                        roomId = item;
                        break;
                    }
                }
                String teacherId = null;
                if (roomId != null) {
                    for (String item: listTeacher) {
                        query.put("teacher_id", item);
                        long count = mongoDBConnection.count(query).orElse(0L);
                        if (count == 0) {
                            teacherId = item;
                        }
                    }
                }
                if (teacherId != null) {
                    Schedule newSchedule = Schedule.builder()
                            .room_id(roomId)
                            .teacher_id(teacherId)
                            .classroom_id(classRoom.get_id().toHexString())
                            .start_date(cStart.getTimeInMillis())
                            .end_date(cEnd.getTimeInMillis())
                            .session(i)
                            .build();
                    schedule.add(newSchedule);
                    i++;
                }
            }
            cStart.add(Calendar.DATE, 1);
            cEnd.add(Calendar.DATE, 1);
        }
        return mongoDBConnection.insertMany(schedule);
    }

    private Long getTime(String value) {
        try {
            String[] sub = value.split(":");
            return (Long.parseLong(sub[0]) * 60 + Long.parseLong(sub[1])) * 1000;
        } catch (Exception e) {
            return 0L;
        }
    }

    private void validateScheduleExits(String classroomId) throws Exception {
        Map<String, Object> query = new HashMap<>();
        query.put("classroom_id", classroomId);
        long count = mongoDBConnection.count(query).orElse(0L);
        if (count > 0) {
            throw new Exception(ExceptionEnum.schedule_exist);
        }
    }

    private List<String> getTeacherIds(String id) {
        List<BasicDBObject> aggregate = Arrays.asList(
                BasicDBObject.parse("{\"$match\": {\"create_date\": {\"$gte\": " + System.currentTimeMillis() + "}, \"teacher_id\": {\"$ne\": \"" + id + "\"}}}"),
                BasicDBObject.parse("{\"$group\": {\"_id\": \"teacher_id\", \"total\": {\"$sum\": 1}}}"),
                BasicDBObject.parse("{\"$sort\": {\"total\": -1}}")
        );
        AggregateIterable<Document> documents = mongoDBConnection.aggregate(aggregate);
        List<String> result = new ArrayList<>();
        if (StringUtils.isNotBlank(id)) {
            result.add(id);
        }
        List<String> result1 = new ArrayList<>();
        if (documents != null) {
            for (Document item : documents) {
                if (item.containsKey("_id") && item.get("_id") != null && StringUtils.isNotBlank(item.get("_id").toString())) {
                    result1.add(item.getString("_id"));
                }
            }
        }
        List<ObjectId> objectIds = result1.stream().map(ObjectId::new).collect(Collectors.toList());
        Map<String, Object> query = new HashMap<>();
        query.put("type", Member.MemberType.TEACHER);
        query.put("_id", new Document("$nin", objectIds));
        result.addAll(memberApplication.find(query).orElse(new ArrayList<>())
                .stream()
                .map(item -> item.get_id().toHexString())
                .collect(Collectors.toList()));
        result.addAll(result1);
        return result;
    }

    private List<String> getRoomIds(String id) {
        List<BasicDBObject> aggregate = Arrays.asList(
                BasicDBObject.parse("{\"$match\": {\"create_date\": {\"$gte\": " + System.currentTimeMillis() + "}, \"room_id\": {\"$ne\": \"" + id + "\"}}}"),
                BasicDBObject.parse("{\"$group\": {\"_id\": \"room_id\", \"total\": {\"$sum\": 1}}}"),
                BasicDBObject.parse("{\"$sort\": {\"total\": -1}}")
        );
        AggregateIterable<Document> documents = mongoDBConnection.aggregate(aggregate);
        List<String> result = new ArrayList<>();
        if (StringUtils.isNotBlank(id)) {
            result.add(id);
        }
        List<String> result1 = new ArrayList<>();
        if (documents != null) {
            for (Document item : documents) {
                if (item.containsKey("_id") && item.get("_id") != null && StringUtils.isNotBlank(item.get("_id").toString())) {
                    result1.add(item.getString("_id"));
                }
            }
        }
        List<ObjectId> objectIds = result1.stream().map(ObjectId::new).collect(Collectors.toList());
        Map<String, Object> query = new HashMap<>();
        query.put("status", Room.RoomStatus.ACTIVE);
        query.put("_id", new Document("$nin", objectIds));
        result.addAll(roomApplication.find(query).orElse(new ArrayList<>())
                .stream()
                .map(item -> item.get_id().toHexString())
                .collect(Collectors.toList()));
        result.addAll(result1);
        return result;
    }

//    public Optional<Paging<Schedule>> getList(CommandSearchSchedule command) throws Exception {
//        Map<String, Object> query = new HashMap<>();
//        if (StringUtils.isNotBlank(command.getKeyword())) {
//            Map<String, Object> $regex = new HashMap<>();
//            $regex.put("$regex", Pattern.compile(command.getKeyword(), Pattern.CASE_INSENSITIVE));
//            query.put("name", $regex);
//        }
//        return mongoDBConnection.find(query, command.getSort(), command.getPage(), command.getSize());
//    }
//
//    public Optional<Schedule> update(CommandAddSchedule command) throws Exception {
//        if(StringUtils.isBlank(command.getId())) {
//            throw new Exception(ExceptionEnum.param_not_null);
//        }
//        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
//            throw new Exception(ExceptionEnum.member_type_deny);
//        }
//        Optional<Schedule> optional = mongoDBConnection.getById(command.getId());
//        if (!optional.isPresent()) {
//            throw new Exception(ExceptionEnum.shift_not_exist);
//        }
//
//        Schedule schedule = optional.get();
//        if (StringUtils.isNotBlank(command.getName())) {
//            schedule.setName(command.getName());
//        }
//        if (StringUtils.isNotBlank(command.getFrom())) {
//            schedule.setFrom(command.getFrom());
//        }
//        if (StringUtils.isNotBlank(command.getTo())) {
//            schedule.setTo(command.getTo());
//        }
//        return mongoDBConnection.update(schedule.get_id().toHexString(), schedule);
//    }
//
//    public Optional<Schedule> getById(String id) {
//        return mongoDBConnection.getById(id);
//    }
//
//    public Optional<List<Schedule>> getAll() {
//        return mongoDBConnection.find(new HashMap<>());
//    }
}
