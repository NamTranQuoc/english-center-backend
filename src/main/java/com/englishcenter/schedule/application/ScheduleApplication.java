package com.englishcenter.schedule.application;

import com.englishcenter.absent.Absent;
import com.englishcenter.absent.AbsentApplication;
import com.englishcenter.classroom.ClassRoom;
import com.englishcenter.classroom.application.ClassRoomApplication;
import com.englishcenter.core.kafka.TopicProducer;
import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.course.Course;
import com.englishcenter.course.application.CourseApplication;
import com.englishcenter.log.Log;
import com.englishcenter.log.LogApplication;
import com.englishcenter.member.Member;
import com.englishcenter.member.application.MemberApplication;
import com.englishcenter.member.command.CommandGetAllByStatusAndType;
import com.englishcenter.member.command.CommandGetAllTeacher;
import com.englishcenter.room.Room;
import com.englishcenter.room.application.RoomApplication;
import com.englishcenter.schedule.Schedule;
import com.englishcenter.schedule.command.CommandAddSchedule;
import com.englishcenter.schedule.command.CommandGetSchedule;
import com.englishcenter.schedule.command.CommandSearchSchedule;
import com.englishcenter.schedule.command.CommandUpdateSchedule;
import com.englishcenter.shift.Shift;
import com.englishcenter.shift.application.ShiftApplication;
import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
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
    @Autowired
    private LogApplication logApplication;
    @Autowired
    private AbsentApplication absentApplication;
    @Autowired
    private KafkaTemplate<String, CommandAddSchedule> kafkaGenerateSchedule;

    public Optional<Schedule> add(CommandAddSchedule command) throws Exception {
        return Optional.empty();
    }

    @KafkaListener(id = "GENERATE_SCHEDULE", topics = TopicProducer.GENERATE_SCHEDULE)
    public void generateEven(CommandAddSchedule command) throws Exception {
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
        if (!ClassRoom.Status.create.equals(classRoom.getStatus())) {
            throw new Exception(ExceptionEnum.can_only_generate_with_status_is_create);
        }
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
        List<String> listTeacher = getTeacherIds(command.getTeacher_id(), course.get_id().toHexString());
        List<String> listRoom = getRoomIds(command.getRoom_id(), classRoom.getMax_student());
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
            int dayOfWeek = cStart.get(Calendar.DAY_OF_WEEK);
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
                    query.remove("room_id");
                    for (String item: listTeacher) {
                        query.put("teacher_id", item);
                        long count = mongoDBConnection.count(query).orElse(0L);
                        if (count == 0) {
                            teacherId = item;
                            break;
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
        classRoom.setStatus("register");
        classRoomApplication.mongoDBConnection.update(classRoom.get_id().toHexString(), classRoom);
        logApplication.mongoDBConnection.insert(Log.builder()
                .class_name(MongodbEnum.collection_schedule)
                .action(Log.ACTION.generate)
                .perform_by(command.getCurrent_member_id())
                .name(classRoom.getName())
                .build());
        mongoDBConnection.insertMany(schedule);
    }

    public Optional<Boolean> generate(CommandAddSchedule command) throws Exception {
        kafkaGenerateSchedule.send(TopicProducer.GENERATE_SCHEDULE, command);
        return Optional.of(Boolean.TRUE);
    }

    public void validateScheduleExits(String classroomId) throws Exception {
        Map<String, Object> query = new HashMap<>();
        query.put("classroom_id", classroomId);
        long count = mongoDBConnection.count(query).orElse(0L);
        if (count > 0) {
            throw new Exception(ExceptionEnum.schedule_exist);
        }
    }

    private List<String> getTeacherIds(String id, String course_id) {
        List<String> full = new ArrayList<>();
        try {
            full = memberApplication.getAllByStatusAndType(CommandGetAllByStatusAndType.builder()
                            .course_id(course_id)
                            .status(Member.MemberStatus.ACTIVE)
                            .type(Member.MemberType.TEACHER)
                    .build()).orElse(new ArrayList<>()).stream().map(CommandGetAllTeacher::get_id).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }
        String listFull = String.join("\", \"", full);
        List<BasicDBObject> aggregate = Arrays.asList(
                BasicDBObject.parse("{\"$match\": {\"start_date\": {\"$gte\": 1638262498990}, \"$and\": [{\"teacher_id\": {\"$ne\": \"" + id + "\"}},{\"teacher_id\": {\"$in\": [\"" + listFull + "\"]}}]}}"),
                BasicDBObject.parse("{\"$group\": {\"_id\": \"$teacher_id\", \"total\": {\"$sum\": 1}}}"),
                BasicDBObject.parse("{\"$sort\": {\"total\": 1}}")
        );
        AggregateIterable<Document> documents = mongoDBConnection.aggregate(aggregate);
        List<String> result = new ArrayList<>();
        if (StringUtils.isNotBlank(id)) {
            result.add(id);
        }
        List<String> result1 = new ArrayList<>();
        try {
            if (documents != null) {
                for (Document item : documents) {
                    if (item.containsKey("_id") && item.get("_id") != null && StringUtils.isNotBlank(item.get("_id").toString())) {
                        result1.add(item.getString("_id"));
                    }
                }
            }
        } catch (Exception e) {

        }
        List<ObjectId> objectIds = result1.stream().map(ObjectId::new).collect(Collectors.toList());
        Map<String, Object> query = new HashMap<>();
        query.put("type", Member.MemberType.TEACHER);
        query.put("course_ids", course_id);
        query.put("status", Member.MemberStatus.ACTIVE);
        query.put("_id", new Document("$nin", objectIds));
        result.addAll(memberApplication.find(query).orElse(new ArrayList<>())
                .stream()
                .map(item -> item.get_id().toHexString())
                .collect(Collectors.toList()));
        result.addAll(result1);
        return result;
    }

    private List<String> getRoomIds(String id, int maxStudent) {
        List<String> result = new ArrayList<>();
        if (StringUtils.isNotBlank(id)) {
            result.add(id);
        }
        Map<String, Object> query = new HashMap<>();
        query.put("status", Room.RoomStatus.ACTIVE);
        query.put("capacity", new Document("$gte", maxStudent));
        Map<String, Object> sort = new HashMap<>();
        sort.put("capacity", 1);
        result.addAll(roomApplication.find(query, sort).orElse(new ArrayList<>())
                .stream()
                .map(item -> item.get_id().toHexString())
                .collect(Collectors.toList()));
        return result;
    }

    public Optional<List<CommandGetSchedule>> gets(CommandSearchSchedule command) throws Exception {
        if (command.getFrom_date() == null || command.getTo_date() == null) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        Map<String, Object> query = new HashMap<>();
        if (Member.MemberType.TEACHER.equals(command.getCurrent_member_role())) {
            query.put("teacher_id", command.getCurrent_member_id());
        }
        if (Member.MemberType.STUDENT.equals(command.getCurrent_member_role())) {
            Map<String, Object> queryRegister = new HashMap<>();
            queryRegister.put("student_ids.student_id", command.getCurrent_member_id());
            List<String> classIds = classRoomApplication.mongoDBConnection.find(queryRegister).orElse(new ArrayList<>())
                    .stream().map(item -> item.get_id().toHexString()).collect(Collectors.toList());
            query.put("classroom_id", new Document("$in", classIds));
        }
        query.put("start_date", new Document("$gte", command.getFrom_date()));
        query.put("end_date", new Document("$lte", command.getTo_date()));
        List<Schedule> list = mongoDBConnection.find(query).orElse(new ArrayList<>());
        Set<String> listClassroomId = list.stream().map(Schedule::getClassroom_id).collect(Collectors.toSet());

        long now = System.currentTimeMillis();
        if (Member.MemberType.STUDENT.equals(command.getCurrent_member_role())) {
            List<String> objectIds = list.stream().map(item -> item.get_id().toHexString()).collect(Collectors.toList());
            Map<String, Object> query1 = new HashMap<>();
            query1.put("schedule_id", new Document("$in", objectIds));
            query1.put("student_id", command.getCurrent_member_id());
            List<Absent> absents = absentApplication.mongoDBConnection.find(query1).orElse(new ArrayList<>());
            if (!CollectionUtils.isEmpty(absents)) {
                Map<String, Absent> absentMap = new HashMap<>();
                for (Absent absent: absents) {
                    absentMap.put(absent.getSchedule_id(), absent);
                }
                absents.stream().map(item -> absentMap.put(item.getSchedule_id(), item));
                for (int i = 0; i < list.size(); i++) {
                    if (absentMap.get(list.get(i).get_id().toHexString()) != null) {
                        Optional<Schedule> schedule1 = mongoDBConnection.getById(absentMap.get(list.get(i).get_id().toHexString()).getBackup_schedule_id());
                        if (schedule1.isPresent()) {
                            list.set(i, schedule1.get());
                            list.get(i).setIs_absent(true);
                            listClassroomId.add(schedule1.get().getClassroom_id());
                        }
                    }
                }
            }
        }

        Map<String, Object> queryClassRoom = new HashMap<>();
        queryClassRoom.put("_id", new Document("$in", listClassroomId.stream().map(ObjectId::new).collect(Collectors.toList())));
        List<ClassRoom> classRooms = classRoomApplication.find(queryClassRoom).orElse(new ArrayList<>());
        Map<String, String> classRoomName = new HashMap<>();
        Map<String, String> classRoomCourseId = new HashMap<>();
        Map<String, Integer> classRoomMaxStudent = new HashMap<>();
        for (ClassRoom classRoom: classRooms) {
            classRoomName.put(classRoom.get_id().toHexString(), classRoom.getName());
            classRoomCourseId.put(classRoom.get_id().toHexString(), classRoom.getCourse_id());
            classRoomMaxStudent.put(classRoom.get_id().toHexString(), classRoom.getMax_student());
        }

        return Optional.of(list.stream().map(item -> CommandGetSchedule.builder()
                .title(classRoomName.get(item.getClassroom_id()))
                .id(item.get_id().toHexString())
                .teacher_id(item.getTeacher_id())
                .room_id(item.getRoom_id())
                .start(item.getStart_date())
                .end(item.getEnd_date())
                .session(item.getSession())
                .max_student(classRoomMaxStudent.get(item.getClassroom_id()))
                .course_id(classRoomCourseId.get(item.getClassroom_id()))
                .took_place(now > item.getStart_date())
                .is_absent(BooleanUtils.isTrue(item.getIs_absent()))
                .classroom_id(item.getClassroom_id())
                .build()).collect(Collectors.toList()));
    }

    public Optional<Schedule> update(CommandUpdateSchedule command) throws Exception {
        if(StringUtils.isAnyBlank(command.getId(), command.getRole())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        Optional<Schedule> optional = mongoDBConnection.getById(command.getId());
        if (!optional.isPresent()) {
            throw new Exception(ExceptionEnum.schedule_not_exist);
        }
        Schedule schedule = optional.get();
        if (System.currentTimeMillis() + 86400000L > schedule.getStart_date()) {
            throw new Exception(ExceptionEnum.can_not_update);
        }
        ClassRoom classRoom = classRoomApplication.getById(schedule.getClassroom_id()).get();
        Map<String, Log.ChangeDetail> changeDetailMap = new HashMap<>();
        if (command.getStart_time() != null && command.getEnd_time() != null && !command.getStart_time().equals(schedule.getStart_date())) {
            changeDetailMap.put("start_date", Log.ChangeDetail.builder()
                    .old_value(schedule.getStart_date().toString())
                    .new_value(command.getStart_time().toString())
                    .build());
            changeDetailMap.put("end_date", Log.ChangeDetail.builder()
                    .old_value(schedule.getEnd_date().toString())
                    .new_value(command.getEnd_time().toString())
                    .build());
            schedule.setStart_date(command.getStart_time());
            schedule.setEnd_date(command.getEnd_time());
        }

        Map<String, Object> query = new HashMap<>();
        query.put("$or", Arrays.asList(
                new Document("$and", Arrays.asList(
                        new Document("start_date", new Document("$lte", schedule.getStart_date())),
                        new Document("end_date", new Document("$gte", schedule.getStart_date()))
                )),
                new Document("$and", Arrays.asList(
                        new Document("start_date", new Document("$lte", schedule.getEnd_date())),
                        new Document("end_date", new Document("$gte", schedule.getEnd_date()))
                ))
        ));
        query.put("classroom_id", schedule.getClassroom_id());
        query.put("_id", new Document("$ne", schedule.get_id()));
        long countV = mongoDBConnection.count(query).orElse(0L);
        if (countV > 0) {
            throw new Exception(ExceptionEnum.schedule_exist);
        }
        query.remove("classroom_id");
        query.remove("_id");
        if (StringUtils.isNotBlank(command.getRoom_id()) && !command.getRoom_id().equals(schedule.getRoom_id())) {
            Optional<Room> room = roomApplication.getById(command.getRoom_id());
            if (!room.isPresent()) {
                throw new Exception(ExceptionEnum.room_not_exist);
            }
            if (room.get().getCapacity() < classRoom.getMax_student()) {
                throw new Exception(ExceptionEnum.room_not_empty);
            }
            query.put("room_id", command.getRoom_id());
            long count = mongoDBConnection.count(query).orElse(0L);
            if (count == 0) {
                changeDetailMap.put("room_id", Log.ChangeDetail.builder()
                        .old_value(schedule.getRoom_id())
                        .new_value(command.getRoom_id())
                        .build());
                schedule.setRoom_id(command.getRoom_id());
            } else {
                throw new Exception(ExceptionEnum.room_not_empty);
            }
        }
        if (StringUtils.isNotBlank(command.getTeacher_id()) && !command.getTeacher_id().equals(schedule.getTeacher_id())) {
            query.remove("room_id");
            Optional<Member> teacher = memberApplication.getById(schedule.getTeacher_id());
            if (!teacher.isPresent()) {
                throw new Exception(ExceptionEnum.member_not_exist);
            }
            query.put("teacher_id", command.getTeacher_id());
            long count = mongoDBConnection.count(query).orElse(0L);
            if (count == 0) {
                changeDetailMap.put("teacher_id", Log.ChangeDetail.builder()
                        .old_value(schedule.getTeacher_id())
                        .new_value(command.getTeacher_id())
                        .build());
                schedule.setTeacher_id(command.getTeacher_id());
            } else {
                throw new Exception(ExceptionEnum.teacher_not_available);
            }
        }
        Map<String, Object> queryUpdate = new HashMap<>();
        queryUpdate.put("classroom_id", schedule.getClassroom_id());
        queryUpdate.put("session", new Document("$gt", schedule.getSession()));
        queryUpdate.put("start_date", new Document("$lt", schedule.getStart_date()));
        Map<String, Object> data = new HashMap<>();
        data.put("$inc", new Document("session", -1));
        long countAdd = mongoDBConnection.updateMany(queryUpdate, data).orElse(0L);
        schedule.setSession(schedule.getSession() + (int) countAdd);
        logApplication.mongoDBConnection.insert(Log.builder()
                .class_name(MongodbEnum.collection_schedule)
                .action(Log.ACTION.update)
                .perform_by(command.getCurrent_member_id())
                .name(classRoom.getName())
                .detail(changeDetailMap)
                .build());
        return mongoDBConnection.update(schedule.get_id().toHexString(), schedule);
    }
}
