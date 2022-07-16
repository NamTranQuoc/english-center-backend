package com.englishcenter.classroom.application;

import com.englishcenter.classroom.ClassRoom;
import com.englishcenter.classroom.command.CommandAddClassRoom;
import com.englishcenter.classroom.command.CommandSearchClassRoom;
import com.englishcenter.core.mail.Mail;
import com.englishcenter.core.mail.MailService;
import com.englishcenter.core.thymeleaf.ThymeleafService;
import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.Paging;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.exam.schedule.ExamSchedule;
import com.englishcenter.log.Log;
import com.englishcenter.log.LogApplication;
import com.englishcenter.member.Member;
import com.englishcenter.member.application.MemberApplication;
import com.englishcenter.room.application.RoomApplication;
import com.englishcenter.schedule.application.ScheduleApplication;
import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class ClassRoomApplication {
    public final MongoDBConnection<ClassRoom> mongoDBConnection;
    @Autowired
    private ScheduleApplication scheduleApplication;
    @Autowired
    private LogApplication logApplication;
    @Autowired
    private MemberApplication memberApplication;
    @Autowired
    private ThymeleafService thymeleafService;
    @Autowired
    private RoomApplication roomApplication;
    @Autowired
    private MailService mailService;

    @Autowired
    public ClassRoomApplication() {
        mongoDBConnection = new MongoDBConnection<>(MongodbEnum.collection_class_room, ClassRoom.class);
    }

    public Optional<ClassRoom> add(CommandAddClassRoom command) throws Exception {
        if (StringUtils.isAnyBlank(command.getName(), command.getCourse_id(), command.getShift_id())
                || command.getMax_student() == null || command.getStart_date() == null
                || CollectionUtils.isEmpty(command.getDow()) || command.getMin_student() == null) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        if (command.getStart_date() < System.currentTimeMillis()) {
            throw new Exception(ExceptionEnum.start_date_not_allow);
        }
        if (mongoDBConnection.checkExistByName(command.getName())) {
            throw new Exception(ExceptionEnum.classroom_exist);
        }
        ClassRoom classRoom = ClassRoom.builder()
                .name(command.getName())
                .course_id(command.getCourse_id())
                .shift_id(command.getShift_id())
                .max_student(command.getMax_student())
                .min_student(command.getMin_student())
                .start_date(command.getStart_date())
                .dow(command.getDow())
                .status(command.getStatus())
                .build();
        logApplication.mongoDBConnection.insert(Log.builder()
                .class_name(MongodbEnum.collection_class_room)
                .action(Log.ACTION.add)
                .perform_by(command.getCurrent_member_id())
                .name(command.getName())
                .build());
        return mongoDBConnection.insert(classRoom);
    }

    public Optional<Paging<ClassRoom>> getList(CommandSearchClassRoom command) throws Exception {
        Map<String, Object> query = new HashMap<>();
        if (StringUtils.isNotBlank(command.getKeyword())) {
            Map<String, Object> $regex = new HashMap<>();
            $regex.put("$regex", Pattern.compile(command.getKeyword(), Pattern.CASE_INSENSITIVE));
            query.put("name", $regex);
        }
        if (!CollectionUtils.isEmpty(command.getShift_ids())) {
            query.put("shift_id", new org.bson.Document("$in", command.getShift_ids()));
        }
        if (!CollectionUtils.isEmpty(command.getCourse_ids())) {
            query.put("course_id", new org.bson.Document("$in", command.getCourse_ids()));
        }
        if (!CollectionUtils.isEmpty(command.getDow())) {
            query.put("dow", new org.bson.Document("$all", command.getDow()));
        }
        if (command.getStart_from_date() != null && command.getStart_to_date() != null) {
            query.put("start_date", new org.bson.Document("$gte", command.getStart_from_date()).append("$lte", command.getStart_to_date()));
        }
        if (!CollectionUtils.isEmpty(command.getStatus())) {
            query.put("status", new Document("$in", command.getStatus()));
        }
        return mongoDBConnection.find(query, command.getSort(), command.getPage(), command.getSize());
    }

    public Optional<ClassRoom> update(CommandAddClassRoom command) throws Exception {
        if (StringUtils.isBlank(command.getId())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        Optional<ClassRoom> optional = mongoDBConnection.getById(command.getId());
        if (!optional.isPresent()) {
            throw new Exception(ExceptionEnum.classroom_not_exist);
        }
        ClassRoom classRoom = optional.get();
        Map<String, Log.ChangeDetail> changeDetailMap = new HashMap<>();
        if (StringUtils.isNotBlank(command.getName()) && !command.getName().equals(classRoom.getName())) {
            if (mongoDBConnection.checkExistByName(command.getName())) {
                throw new Exception(ExceptionEnum.classroom_exist);
            }
            changeDetailMap.put("name", Log.ChangeDetail.builder()
                    .old_value(classRoom.getName())
                    .new_value(command.getName())
                    .build());
            classRoom.setName(command.getName());
        }
        if (command.getMax_student() != null && !command.getMax_student().equals(classRoom.getMax_student())) {
            changeDetailMap.put("max_student", Log.ChangeDetail.builder()
                    .old_value(classRoom.getMax_student().toString())
                    .new_value(command.getMax_student().toString())
                    .build());
            classRoom.setMax_student(command.getMax_student());
        }
        if (command.getMin_student() != null && !command.getMin_student().equals(classRoom.getMin_student())) {
            changeDetailMap.put("min_student", Log.ChangeDetail.builder()
                    .old_value(classRoom.getMin_student().toString())
                    .new_value(command.getMin_student().toString())
                    .build());
            classRoom.setMin_student(command.getMin_student());
        }
        if (command.getStatus() != null && !command.getStatus().equals(classRoom.getStatus())) {
            if (!ClassRoom.Status.create.equals(classRoom.getStatus())) {
                throw new Exception(ExceptionEnum.cannot_when_status_not_is_create);
            }
            changeDetailMap.put("status", Log.ChangeDetail.builder()
                    .old_value(classRoom.getStatus())
                    .new_value(command.getStatus())
                    .build());
            classRoom.setStatus(command.getStatus());
        }
        if (command.getStart_date() != null && !command.getStart_date().equals(classRoom.getStart_date())) {
            if (classRoom.getStart_date() < System.currentTimeMillis() || command.getStart_date() < System.currentTimeMillis()) {
                throw new Exception(ExceptionEnum.start_date_not_allow);
            }
            if (!ClassRoom.Status.register.equals(classRoom.getStatus())) {
                throw new Exception(ExceptionEnum.cannot_when_status_not_is_register);
            }
            scheduleApplication.validateScheduleExits(classRoom.get_id().toHexString());
            changeDetailMap.put("start_date", Log.ChangeDetail.builder()
                    .old_value(classRoom.getStart_date().toString())
                    .new_value(command.getStart_date().toString())
                    .build());
            classRoom.setStart_date(command.getStart_date());
        }
        logApplication.mongoDBConnection.insert(Log.builder()
                .class_name(MongodbEnum.collection_class_room)
                .action(Log.ACTION.update)
                .perform_by(command.getCurrent_member_id())
                .name(command.getName())
                .detail(changeDetailMap)
                .build());
        return mongoDBConnection.update(classRoom.get_id().toHexString(), classRoom);
    }

    public Optional<ClassRoom> getById(String id) {
        return mongoDBConnection.getById(id);
    }

    public Optional<List<ClassRoom>> getAll() {
        return mongoDBConnection.find(new HashMap<>());
    }

    public Optional<List<ClassRoom>> find(Map<String, Object> query) {
        return mongoDBConnection.find(query);
    }

    public Optional<List<ClassRoom>> getClassByCourseId(String id) {
        return mongoDBConnection.find(new Document("course_id", id).append("status", "register"));
    }

//    public void sendMailRemind() {
//        try {
//            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
//            long now = System.currentTimeMillis();
//            String sNow = formatter.format(new Date());
//            Map<String, Object> query = new HashMap<>();
//            query.put("start_date", new Document("$gte", now).append("$lte", now + 86400000L));
//            List<Schedule> schedules = scheduleApplication.mongoDBConnection.find(query).orElse(new ArrayList<>());
//            for (Schedule schedule: schedules) {
//                Optional<Room> room = roomApplication.getById(schedule.getRoom_id());
//                Optional<ClassRoom> classroom = mongoDBConnection.getById(schedule.getClassroom_id());
//                if (room.isPresent() && classroom.isPresent()) {
//                    Map<String, Object> data = new HashMap<>();
//                    Calendar calendar = Calendar.getInstance();
//                    calendar.setTimeInMillis(schedule.getStart_date());
//
//                    data.put("classroom", classroom.get().getName());
//                    data.put("room", room.get().getName());
//                    data.put("start_date", String.format("%s %02dh%02d", sNow, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE)));
//
//                    List<ObjectId> ids = classroom.get().getStudent_ids().stream().map(item -> new ObjectId(item.getStudent_id())).collect(Collectors.toList());
//                    List<String> students = memberApplication.mongoDBConnection.find(new Document("_id", new Document("$in", ids)))
//                            .orElse(new ArrayList<>())
//                            .stream().map(Member::getEmail).collect(Collectors.toList());
//                    if (!CollectionUtils.isEmpty(students)) {
//                        kafkaEmail.send(TopicProducer.SEND_MAIL, Mail.builder()
//                                .mail_tos(students)
//                                .mail_subject("Thông báo!")
//                                .mail_content(thymeleafService.getContent("mailRemindSchedule", data))
//                                .build());
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public void updateStatusExam() {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            formatter.setTimeZone(TimeZone.getTimeZone("GMT+07:00"));
            long now = System.currentTimeMillis() + 172800000L;
            Map<String, Object> query = new HashMap<>();
            query.put("start_date", new Document("$gte", now).append("$lte", now + 86400000L));
            query.put("status", ExamSchedule.ExamStatus.register);
            List<String> ids = new ArrayList<>();
            List<ClassRoom> classRooms = mongoDBConnection.find(query).orElse(new ArrayList<>());
            List<ObjectId> cancelIds = new ArrayList<>();
            List<ObjectId> comingIds = new ArrayList<>();
            for (ClassRoom classRoom : classRooms) {
                if (classRoom.getMin_student() > classRoom.getStudent_ids().size()) {
                    ids.addAll(classRoom.getStudent_ids().stream().map(ClassRoom.StudentRegister::getStudent_id).collect(Collectors.toList()));
                    cancelIds.add(classRoom.get_id());
                } else {
                    comingIds.add(classRoom.get_id());
                }
            }
            if (!CollectionUtils.isEmpty(ids)) {
                Map<String, Object> data = new HashMap<>();
                data.put("reason", "Lớp học của bạn bắt đầu vào ngày " + formatter.format(new Date(now)) + " đã bị hủy do không đủ số lượng đăng ký");
                List<ObjectId> objectIds = ids.stream().map(ObjectId::new).collect(Collectors.toList());
                List<String> students = memberApplication.mongoDBConnection.find(new Document("_id", new Document("$in", objectIds)))
                        .orElse(new ArrayList<>())
                        .stream().map(Member::getEmail).collect(Collectors.toList());
                mailService.send(Mail.builder()
                        .mail_tos(students)
                        .mail_subject("Thông báo!")
                        .mail_content(thymeleafService.getContent("mailWhenCancel", data))
                        .build());
            }
            Map<String, Object> queryUpdate = new HashMap<>();
            Map<String, Object> dataUpdate = new HashMap<>();
            queryUpdate.put("_id", new Document("$in", cancelIds));
            dataUpdate.put("status", ExamSchedule.ExamStatus.cancel);
            mongoDBConnection.update(queryUpdate, new Document("$set", dataUpdate));
            Map<String, Object> queryDelete = new HashMap<>();
            queryDelete.put("classroom_id", new Document("$in", cancelIds.stream().map(ObjectId::toHexString).collect(Collectors.toList())));
            scheduleApplication.mongoDBConnection.delete(queryDelete);
            queryUpdate.put("_id", new Document("$in", comingIds));
            dataUpdate.put("status", ExamSchedule.ExamStatus.coming);
            mongoDBConnection.update(queryUpdate, new Document("$set", dataUpdate));

            List<String> idsCancel = cancelIds.stream().map(ObjectId::toHexString).collect(Collectors.toList());
            Map<String, Object> deleteQuery = new HashMap<>();
            deleteQuery.put("classroom_id", new Document("$in", idsCancel));
            scheduleApplication.mongoDBConnection.delete(deleteQuery);

            List<BasicDBObject> aggregate = Collections.singletonList(
                    BasicDBObject.parse("{\"$group\": {\"_id\": \"$classroom_id\", \"current\": {\"$sum\": {\"$cond\": {\"if\": {\"$lte\": [\"$end_date\", " + System.currentTimeMillis() + "]}, \"then\": 1, \"else\": 0}}},\"max\": {\"$sum\": 1}}}")
            );
            AggregateIterable<Document> documents = mongoDBConnection.aggregate(aggregate);
            List<ObjectId> finishIds = new ArrayList<>();
            if (documents != null) {
                for (Document item : documents) {
                    if (item.containsKey("_id") && item.get("_id") != null && StringUtils.isNotBlank(item.get("_id").toString())) {
                        if (item.getInteger("current", 0) >= item.getInteger("max", 0)) {
                            finishIds.add(new ObjectId(item.getString("_id")));
                        }
                    }
                }
            }
            queryUpdate.put("_id", new Document("$in", finishIds));
            queryUpdate.put("status", ExamSchedule.ExamStatus.coming);
            dataUpdate.put("status", ExamSchedule.ExamStatus.finish);
            mongoDBConnection.update(queryUpdate, new Document("$set", dataUpdate));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Optional<List<ClassRoom>> getByKeyWord(String keyword) {
        Map<String, Object> query = new HashMap<>();
        Map<String, Object> $regex = new HashMap<>();
        $regex.put("$regex", Pattern.compile(keyword, Pattern.CASE_INSENSITIVE));
        query.put("name", $regex);

        return mongoDBConnection.find(query);
    }
}
