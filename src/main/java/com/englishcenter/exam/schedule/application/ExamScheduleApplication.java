package com.englishcenter.exam.schedule.application;

import com.englishcenter.code.CodeApplication;
import com.englishcenter.core.firebase.FirebaseFileService;
import com.englishcenter.core.kafka.TopicProducer;
import com.englishcenter.core.mail.Mail;
import com.englishcenter.core.schedule.ScheduleName;
import com.englishcenter.core.schedule.TaskSchedulingService;
import com.englishcenter.core.thymeleaf.ThymeleafService;
import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.Paging;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.exam.schedule.ExamSchedule;
import com.englishcenter.exam.schedule.command.CommandAddExamSchedule;
import com.englishcenter.exam.schedule.command.CommandRegisterExam;
import com.englishcenter.exam.schedule.command.CommandSearchExamSchedule;
import com.englishcenter.exam.schedule.job.ExamScheduleRemindJob;
import com.englishcenter.member.Member;
import com.englishcenter.member.application.MemberApplication;
import com.englishcenter.room.Room;
import com.englishcenter.room.application.RoomApplication;
import com.englishcenter.schedule.application.ScheduleApplication;
import com.englishcenter.schedule.job.ScheduleRemindJob;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class ExamScheduleApplication {
    public final MongoDBConnection<ExamSchedule> mongoDBConnection;

    private final long TEN_MINUTE = 600000;
    @Autowired
    private ScheduleApplication scheduleApplication;
    @Autowired
    private RoomApplication roomApplication;
    @Autowired
    private MemberApplication memberApplication;
    @Autowired
    private FirebaseFileService firebaseFileService;
    @Autowired
    private ThymeleafService thymeleafService;
    @Autowired
    private CodeApplication codeApplication;
    @Autowired
    private KafkaTemplate<String, Mail> kafkaEmail;
    @Autowired
    private TaskSchedulingService taskSchedulingService;
    @Autowired
    public ExamScheduleApplication() {
        mongoDBConnection = new MongoDBConnection<>(MongodbEnum.collection_exam_schedule, ExamSchedule.class);
    }

    public Optional<ExamSchedule> add(CommandAddExamSchedule command) throws Exception {
        if (StringUtils.isAnyBlank(command.getRoom_id())
                || command.getStart_time() == null
                || command.getEnd_time() == null
                || command.getMember_ids().isEmpty()
                || command.getMin_quantity() == null
                || command.getMax_quantity() == null) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        validateEnoughRoom(command);
        Map<String, Object> query = new HashMap<>();
        query.put("$or", Arrays.asList(
                new Document("$and", Arrays.asList(
                        new Document("start_time", new Document("$lte", command.getStart_time())),
                        new Document("end_time", new Document("$gte", command.getStart_time()))
                )),
                new Document("$and", Arrays.asList(
                        new Document("start_time", new Document("$lte", command.getEnd_time())),
                        new Document("end_time", new Document("$gte", command.getEnd_time()))
                ))
        ));
        query.put("room_id", command.getRoom_id());
        long count = mongoDBConnection.count(query).orElse(0L) + scheduleApplication.mongoDBConnection.count(query).orElse(0L);
        if (count != 0L) {
            throw new Exception(ExceptionEnum.room_not_available);
        }
        query.remove("room_id");
        query.put("member_ids", new Document("$in", command.getMember_ids()));
        count = mongoDBConnection.count(query).orElse(0L);
        if (count != 0L) {
            throw new Exception(ExceptionEnum.receptionist_not_available);
        }
        ExamSchedule examSchedule = ExamSchedule.builder()
                .start_time(command.getStart_time())
                .end_time(command.getEnd_time())
                .room_id(command.getRoom_id())
                .member_ids(command.getMember_ids())
                .min_quantity(command.getMin_quantity())
                .max_quantity(command.getMax_quantity())
                .code(codeApplication.generateCodeByType("exam"))
                .build();
        Optional<ExamSchedule> result = mongoDBConnection.insert(examSchedule);
        if (result.isPresent()) {
            ExamScheduleRemindJob examScheduleRemindJob = new ExamScheduleRemindJob();
            examScheduleRemindJob.setExamScheduleId(result.get().get_id().toHexString());
            examScheduleRemindJob.setKafkaEmail(kafkaEmail);
            examScheduleRemindJob.setTaskSchedulingService(taskSchedulingService);
            taskSchedulingService.scheduleATask(
                    examScheduleRemindJob,
                    result.get().getStart_time() - TEN_MINUTE,
                    ScheduleName.EXAM_SCHEDULE_REMIND,
                    result.get().get_id().toHexString());
        }
        return result;
    }

    public Optional<Boolean> register(CommandRegisterExam command) throws Exception {
        if (StringUtils.isAnyBlank(command.getExam_id(), command.getMember())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        Optional<ExamSchedule> optionalExamSchedule = mongoDBConnection.getById(command.getExam_id());
        if (!optionalExamSchedule.isPresent()) {
            throw new Exception(ExceptionEnum.exam_schedule_not_exist);
        }
        ExamSchedule examSchedule = optionalExamSchedule.get();
        if (!ExamSchedule.ExamStatus.register.equals(examSchedule.getStatus())) {
            throw new Exception(ExceptionEnum.register_not_in_time_register);
        }
        Map<String, Object> query = new HashMap<>();
        List<Document> or = new ArrayList<>(Arrays.asList(
                new Document("email", command.getMember()),
                new Document("code", command.getMember()),
                new Document("phone_number", command.getMember())
        ));
        try {
            or.add(new Document("_id", new ObjectId(command.getMember())));
        } catch (Exception e) {
//            e.printStackTrace();
        }
        query.put("$or", or);
        query.put("status", Member.MemberStatus.ACTIVE);
        query.put("type", Member.MemberType.STUDENT);
        Optional<Member> optionalMember = memberApplication.mongoDBConnection.findOne(query);
        if (!optionalMember.isPresent()) {
            throw new Exception(ExceptionEnum.member_not_exist);
        }
        Member student = optionalMember.get();

        if (examSchedule.getStudent_ids().contains(student.get_id().toHexString())) {
            throw new Exception(ExceptionEnum.register_already);
        }

        Map<String, Object> queryValidate = new HashMap<>();
        queryValidate.put("$or", Arrays.asList(
                new Document("$and", Arrays.asList(
                        new Document("start_time", new Document("$lte", examSchedule.getStart_time())),
                        new Document("end_time", new Document("$gte", examSchedule.getStart_time()))
                )),
                new Document("$and", Arrays.asList(
                        new Document("start_time", new Document("$lte", examSchedule.getEnd_time())),
                        new Document("end_time", new Document("$gte", examSchedule.getEnd_time()))
                ))
        ));
        queryValidate.put("student_ids", student.get_id().toHexString());
        long count = mongoDBConnection.count(queryValidate).orElse(0L);
        if (count != 0L) {
            throw new Exception(ExceptionEnum.exam_schedule_conflict);
        }
        examSchedule.getStudent_ids().add(student.get_id().toHexString());
        mongoDBConnection.update(examSchedule.get_id().toHexString(), examSchedule);
        return Optional.of(Boolean.TRUE);
    }

    public Optional<Paging<ExamSchedule>> getList(CommandSearchExamSchedule command) throws Exception {
        Map<String, Object> query = new HashMap<>();
        if (StringUtils.isNotBlank(command.getKeyword())) {
            Map<String, Object> $regex = new HashMap<>();
            $regex.put("$regex", Pattern.compile(command.getKeyword(), Pattern.CASE_INSENSITIVE));
            query.put("name", $regex);
        }
        if (!CollectionUtils.isEmpty(command.getMember_ids())) {
            query.put("member_ids", new org.bson.Document("$in", command.getMember_ids()));
        }
        if (!CollectionUtils.isEmpty(command.getStatuses())) {
            query.put("status", new org.bson.Document("$in", command.getStatuses()));
        }
        if (command.getRoom_id() != null) {
            query.put("room_id", new org.bson.Document("$in", command.getRoom_id()));
        }
        if (!CollectionUtils.isEmpty(command.getRoom_ids())) {
            query.put("room_id", new org.bson.Document("$in", command.getRoom_ids()));
        }
        if (command.getStart_time() != null) {
            query.put("start_time", new org.bson.Document("$gte", command.getStart_time()).append("$lte", command.getEnd_time()));
        }
        return mongoDBConnection.find(query, command.getSort(), command.getPage(), command.getSize());
    }

    public Optional<String> exportExcel(String id) throws Exception {
        if (StringUtils.isBlank(id)) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        Optional<ExamSchedule> optional = mongoDBConnection.getById(id);
        if (!optional.isPresent()) {
            throw new Exception(ExceptionEnum.exam_schedule_not_exist);
        }
        ExamSchedule examSchedule = optional.get();
        List<ObjectId> memberIds = examSchedule.getStudent_ids().stream().map(ObjectId::new).collect(Collectors.toList());
        Map<String, Object> query = new HashMap<>();
        query.put("_id", new Document("$in", memberIds));
        List<Member> members = memberApplication.find(query).orElse(new ArrayList<>());
        FileOutputStream fileOutputStream = null;
        SXSSFWorkbook workbook = new SXSSFWorkbook();
        ;
        String filePath = "export-exam.xlsx";
        createTemplateExport(filePath, workbook, members);
        try {
            fileOutputStream = new FileOutputStream(filePath);
            workbook.write(fileOutputStream);
            fileOutputStream.close();
            workbook.close();
            File file = new File(filePath);
            firebaseFileService.save(file, filePath);
            file.delete();
            return Optional.of(firebaseFileService.getDownloadUrl(filePath, "exports"));
        } catch (Exception e) {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return Optional.empty();
    }

    private void setCellHeader(SXSSFRow row, CellStyle headerCellStyle, String title, int col_index) {
        SXSSFCell cell = row.createCell(col_index);
        cell.setCellStyle(headerCellStyle);
        cell.setCellValue(title);
    }

    private CellStyle createCellStyle(SXSSFWorkbook workbook) {
        CellStyle headerCellStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerCellStyle.setFont(headerFont);
        headerCellStyle.setAlignment(HorizontalAlignment.CENTER);
        headerCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        return headerCellStyle;
    }

    private void setColumnWidth(SXSSFSheet sheet, Integer... col_indexs) {
        for (int i = 0; i < col_indexs.length; ++i) {
            sheet.setColumnWidth(i, col_indexs[i] * 1000);
        }
    }

    public void createTemplateExport(String pathName, SXSSFWorkbook workbook, List<Member> students) {
        try {
            if (org.apache.commons.collections4.CollectionUtils.isEmpty(students)) return;
            SXSSFSheet sheet = workbook.createSheet(pathName);
            int col_index = 0;
            int row_index = 0;

            SXSSFRow row = sheet.createRow(row_index);

            this.setCellHeader(row, this.createCellStyle(workbook), "STT", col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Mã Học viên", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Họ và tên", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "SĐT", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Email", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Ngày sinh", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Giới tính", ++col_index);

            this.setColumnWidth(sheet, 2, 5, 7, 4, 7, 4, 4, 4);
            SXSSFCell cell = null;
            for (Member member : students) {
                col_index = 0;
                row = sheet.createRow(++row_index);

                //STT
                cell = row.createCell(col_index);
                cell.setCellValue(row_index);

                //Mã
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getCode()) ? member.getCode() : "");

                //Họ và tên
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getName()) ? member.getName() : "");

                //SĐT
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getPhone_number()) ? member.getPhone_number() : "");

                //Email
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getEmail()) ? member.getEmail() : "");

                //Ngày sinh
                cell = row.createCell(++col_index);
                cell.setCellValue(member.getDob() != null ? new SimpleDateFormat("dd/MM/yyyy").format(new Date(member.getDob())) : "_");

                //Giới tính
                cell = row.createCell(++col_index);
                cell.setCellValue("female".equals(member.getGender()) ? "Nữ" : "male".equals(member.getGender()) ? "Nam" : "Khác");
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public Optional<ExamSchedule> update(CommandAddExamSchedule command) throws Exception {
        if (StringUtils.isBlank(command.getId())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        Optional<ExamSchedule> optional = mongoDBConnection.getById(command.getId());
        if (!optional.isPresent()) {
            throw new Exception(ExceptionEnum.exam_schedule_not_exist);
        }
        ExamSchedule examSchedule = optional.get();
        if (System.currentTimeMillis() + 86400000L > command.getStart_time()) {
            throw new Exception(ExceptionEnum.can_not_update);
        }
        Map<String, Object> query = new HashMap<>();
        query.put("$or", Arrays.asList(
                new Document("$and", Arrays.asList(
                        new Document("start_time", new Document("$lte", command.getStart_time())),
                        new Document("end_time", new Document("$gte", command.getStart_time()))
                )),
                new Document("$and", Arrays.asList(
                        new Document("start_time", new Document("$lte", command.getEnd_time())),
                        new Document("end_time", new Document("$gte", command.getEnd_time()))
                ))
        ));
        if (StringUtils.isNotBlank(command.getRoom_id()) && !command.getRoom_id().equals(examSchedule.getRoom_id())) {
            validateEnoughRoom(command);
            query.put("room_id", command.getRoom_id());
            long count = mongoDBConnection.count(query).orElse(0L) + scheduleApplication.mongoDBConnection.count(query).orElse(0L);
            if (count != 0L) {
                throw new Exception(ExceptionEnum.room_not_available);
            }
            examSchedule.setRoom_id(command.getRoom_id());
        }
        if (command.getStart_time() != null) {
            examSchedule.setStart_time(command.getStart_time());
        }
        if (command.getEnd_time() != null) {
            examSchedule.setEnd_time(command.getEnd_time());
        }
        if (command.getMax_quantity() != null) {
            examSchedule.setMax_quantity(command.getMax_quantity());
        }
        if (command.getMin_quantity() != null) {
            examSchedule.setMin_quantity(command.getMin_quantity());
        }
        if (!CollectionUtils.isEmpty(command.getMember_ids())) {
            query.remove("room_id");
            List<String> ids = command.getMember_ids().stream()
                    .filter(item -> !examSchedule.getMember_ids().contains(item))
                    .collect(Collectors.toList());
            query.put("member_ids", new Document("$in", ids));
            long count = mongoDBConnection.count(query).orElse(0L);
            if (count != 0L) {
                throw new Exception(ExceptionEnum.receptionist_not_available);
            }
            examSchedule.setMember_ids(command.getMember_ids());
        }
        taskSchedulingService.removeScheduledTask(ScheduleName.EXAM_SCHEDULE_REMIND, command.getId());
        ExamScheduleRemindJob examScheduleRemindJob = new ExamScheduleRemindJob();
        examScheduleRemindJob.setExamScheduleId(command.getId());
        examScheduleRemindJob.setKafkaEmail(kafkaEmail);
        examScheduleRemindJob.setTaskSchedulingService(taskSchedulingService);
        taskSchedulingService.scheduleATask(examScheduleRemindJob, examSchedule.getStart_time() - TEN_MINUTE, ScheduleName.EXAM_SCHEDULE_REMIND, command.getId());
        return mongoDBConnection.update(examSchedule.get_id().toHexString(), examSchedule);
    }

    private void validateEnoughRoom(CommandAddExamSchedule command) throws Exception {
        Optional<Room> optionalRoom = roomApplication.getById(command.getRoom_id());
        if (!optionalRoom.isPresent()) {
            throw new Exception(ExceptionEnum.room_not_exist);
        }
        Room room = optionalRoom.get();
        if (room.getCapacity() < command.getMax_quantity()) {
            throw new Exception(ExceptionEnum.room_capacity_is_not_enough);
        }
    }

    public Optional<List<ExamSchedule>> find(Map<String, Object> query, Map<String, Object> sort) {
        return mongoDBConnection.find(query, sort);
    }

    public Optional<List<ExamSchedule>> getAll() {
        return mongoDBConnection.find(new HashMap<>());
    }

    public Optional<ExamSchedule> getById(String id) {
        return mongoDBConnection.getById(id);
    }

    public void sendMailRemind() {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            long now = System.currentTimeMillis();
            String sNow = formatter.format(new Date());
            Map<String, Object> query = new HashMap<>();
            query.put("start_time", new Document("$gte", now).append("$lte", now + 86400000L));
            List<ExamSchedule> examSchedules = mongoDBConnection.find(query).orElse(new ArrayList<>());
            for (ExamSchedule examSchedule : examSchedules) {
                Optional<Room> room = roomApplication.getById(examSchedule.getRoom_id());
                if (room.isPresent()) {
                    Map<String, Object> data = new HashMap<>();
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(examSchedule.getStart_time());

                    data.put("date", sNow);
                    data.put("room", room.get().getName());
                    data.put("start_date", String.format("%02dh%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE)));

                    List<ObjectId> ids = examSchedule.getStudent_ids().stream().map(ObjectId::new).collect(Collectors.toList());
                    ids.addAll(examSchedule.getMember_ids().stream().map(ObjectId::new).collect(Collectors.toList()));
                    List<String> students = memberApplication.mongoDBConnection.find(new Document("_id", new Document("$in", ids)))
                            .orElse(new ArrayList<>())
                            .stream().map(Member::getEmail).collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(students)) {
                        kafkaEmail.send(TopicProducer.SEND_MAIL, Mail.builder()
                                .mail_tos(students)
                                .mail_subject("Thông báo!")
                                .mail_content(thymeleafService.getContent("mailRemindExam", data))
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateStatusExam() {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            long now = System.currentTimeMillis() + 172800000L;
            Map<String, Object> query = new HashMap<>();
            query.put("start_time", new Document("$gte", now).append("$lte", now + 86400000L));
            query.put("status", ExamSchedule.ExamStatus.register);
            List<String> ids = new ArrayList<>();
            List<ExamSchedule> examSchedules = mongoDBConnection.find(query).orElse(new ArrayList<>());
            List<ObjectId> cancelIds = new ArrayList<>();
            List<ObjectId> comingIds = new ArrayList<>();
            for (ExamSchedule examSchedule : examSchedules) {
                if (examSchedule.getMin_quantity() > examSchedule.getStudent_ids().size()) {
                    examSchedule.setStatus(ExamSchedule.ExamStatus.cancel);
                    ids.addAll(examSchedule.getStudent_ids());
                    cancelIds.add(examSchedule.get_id());
                } else {
                    examSchedule.setStatus(ExamSchedule.ExamStatus.coming);
                    comingIds.add(examSchedule.get_id());
                }
            }
            Map<String, Object> data = new HashMap<>();
            data.put("reason", "Lịch thi của bạn vào ngày " + formatter.format(new Date(now)) + " đã bị hủy do không đủ số lượng đăng ký");
            List<ObjectId> objectIds = ids.stream().map(ObjectId::new).collect(Collectors.toList());
            List<String> students = memberApplication.mongoDBConnection.find(new Document("_id", new Document("$in", objectIds)))
                    .orElse(new ArrayList<>())
                    .stream().map(Member::getEmail).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(students)) {
                kafkaEmail.send(TopicProducer.SEND_MAIL, Mail.builder()
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
            queryUpdate.put("_id", new Document("$in", comingIds));
            dataUpdate.put("status", ExamSchedule.ExamStatus.coming);
            mongoDBConnection.update(queryUpdate, new Document("$set", dataUpdate));

            now = System.currentTimeMillis();
            queryUpdate.remove("_id");
            queryUpdate.put("start_time", new Document("$lte", now));
            queryUpdate.put("status", new Document("$ne", ExamSchedule.ExamStatus.cancel));
            dataUpdate.put("status", ExamSchedule.ExamStatus.finish);
            mongoDBConnection.update(queryUpdate, new Document("$set", dataUpdate));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Optional<List<ExamSchedule>> getRegister() {
        Map<String, Object> query = new HashMap<>();
        query.put("status", ExamSchedule.ExamStatus.register);

        return mongoDBConnection.find(query);
    }
}
