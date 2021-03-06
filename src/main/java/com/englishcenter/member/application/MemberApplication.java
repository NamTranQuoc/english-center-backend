package com.englishcenter.member.application;

import com.englishcenter.auth.application.IAuthApplication;
import com.englishcenter.code.CodeApplication;
import com.englishcenter.core.firebase.IFirebaseFileService;
import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.Paging;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.course.application.CourseApplication;
import com.englishcenter.course.command.CommandGetAllCourse;
import com.englishcenter.member.Member;
import com.englishcenter.member.async.UpdateScoreAsync;
import com.englishcenter.member.command.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
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
public class MemberApplication implements IMemberApplication {
    public final MongoDBConnection<Member> mongoDBConnection;
    @Autowired
    private IAuthApplication authApplication;
    @Autowired
    private IFirebaseFileService firebaseFileService;
    @Autowired
    private CodeApplication codeApplication;
    @Autowired
    private CourseApplication courseApplication;
    @Autowired
    private UpdateScoreAsync updateScoreAsync;

    @Autowired
    public MemberApplication() {
        mongoDBConnection = new MongoDBConnection<>(MongodbEnum.collection_member, Member.class);
    }

    @Override
    public Optional<List<Member>> find(Map<String, Object> query) {
        query.put("is_deleted", false);
        return mongoDBConnection.find(query);
    }

    @Override
    public Optional<Paging<Member>> getList(CommandSearchMember command) throws Exception {
        validateRoleAdminAndReceptionist(command.getMember_type(), command.getTypes());
        Map<String, Object> query = getQueryMember(command);
        return mongoDBConnection.find(query, command.getSort(), command.getPage(), command.getSize());
    }

    @Override
    public Optional<List<CommandGetAllTeacher>> getAllByStatusAndType(CommandGetAllByStatusAndType command) throws Exception {
        if (StringUtils.isAnyBlank(command.getStatus(), command.getType())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        Map<String, Object> query = new HashMap<>();
        query.put("status", command.getStatus());
        query.put("type", command.getType());
        if (StringUtils.isNotBlank(command.getCourse_id())) {
            query.put("course_ids", command.getCourse_id());
        }
        Map<String, Object> sort = new HashMap<>();
        sort.put("_id", 1);
        List<Member> list = mongoDBConnection.find(query, sort).orElse(new ArrayList<>());
        return Optional.of(list.stream().map(item -> CommandGetAllTeacher.builder()
                ._id(item.get_id().toHexString())
                .name(item.getName())
                .build()).collect(Collectors.toList()));
    }

    private Map<String, Object> getQueryMember(CommandSearchMember command) {
        Map<String, Object> query = new HashMap<>();
        query.put("is_deleted", false);
        if (!CollectionUtils.isEmpty(command.getTypes())) {
            query.put("type", new Document("$in", command.getTypes()));
        }
        if (StringUtils.isNotBlank(command.getKeyword())) {
            Map<String, Object> $regex = new HashMap<>();
            $regex.put("$regex", Pattern.compile(command.getKeyword(), Pattern.CASE_INSENSITIVE));
            query.put("$or", Arrays.asList(
                    new Document("name", $regex),
                    new Document("email", $regex),
                    new Document("code", command.getKeyword())
            ));
        }
        if (command.getFrom_date() != null && command.getTo_date() != null) {
            query.put("create_date", new Document("$gte", command.getFrom_date()).append("$lte", command.getTo_date()));
        }
        if (!CollectionUtils.isEmpty(command.getGenders())) {
            query.put("gender", new Document("$in", command.getGenders()));
        }
        if (!CollectionUtils.isEmpty(command.getCourse_ids())) {
            query.put("course_ids", new Document("$in", command.getCourse_ids()));
        }
        if (command.getFrom_dob_date() != null && command.getTo_dob_date() != null) {
            query.put("dob", new Document("$gte", command.getFrom_dob_date()).append("$lte", command.getTo_dob_date()));
        }
        if (command.getMin_input_score() != null && command.getMax_input_score() != null) {
            query.put("input_score.total", new Document("$gte", command.getMin_input_score()).append("$lte", command.getMax_input_score()));
        }
        if (command.getMin_output_score() != null && command.getMax_output_score() != null) {
            query.put("current_score.total", new Document("$gte", command.getMin_output_score()).append("$lte", command.getMax_output_score()));
        }
        return query;
    }

    @Override
    public Optional<String> export(CommandSearchMember command) throws Exception {
        SXSSFWorkbook workbook = new SXSSFWorkbook();
        String filePath = "export-" + command.getTypes().get(0) + ".xlsx";
        Map<String, Object> query = getQueryMember(command);
        List<Member> students = mongoDBConnection.find(query).orElse(new ArrayList<>());
        if (command.getTypes().contains(Member.MemberType.STUDENT)) {
            createTemplateExportStudent(filePath, workbook, students);
            if (exportFile(workbook, filePath))
                return Optional.of(firebaseFileService.getDownloadUrl(filePath, "exports"));
        } else if (command.getTypes().contains(Member.MemberType.TEACHER)) {
            createTemplateExportTeacher(filePath, workbook, students);
            if (exportFile(workbook, filePath))
                return Optional.of(firebaseFileService.getDownloadUrl(filePath, "exports"));
        } else if (command.getTypes().contains(Member.MemberType.RECEPTIONIST)) {
            createTemplateExportReceptionist(filePath, workbook, students);
            if (exportFile(workbook, filePath))
                return Optional.of(firebaseFileService.getDownloadUrl(filePath, "exports"));
        }
        return Optional.empty();
    }

    private boolean exportFile(SXSSFWorkbook workbook, String filePath) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(filePath);
            workbook.write(fileOutputStream);
            fileOutputStream.close();
            workbook.close();
            File file = new File(filePath);
            firebaseFileService.save(file, filePath);
            file.delete();
            return true;
        } catch (Exception e) {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return false;
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

    private void createTemplateExportReceptionist(String pathName, SXSSFWorkbook workbook, List<Member> students) {
        try {
            if (org.apache.commons.collections4.CollectionUtils.isEmpty(students)) return;
            SXSSFSheet sheet = workbook.createSheet(pathName);
            int col_index = 0;
            int row_index = 0;

            SXSSFRow row = sheet.createRow(row_index);

            this.setCellHeader(row, this.createCellStyle(workbook), "STT", col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "M?? Nh??n vi??n", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "H??? v?? t??n", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "S??T", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Email", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Ng??y sinh", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Gi???i t??nh", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Tr???ng th??i", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "?????a ch???", ++col_index);

            this.setColumnWidth(sheet, 2, 5, 7, 4, 7, 4, 4, 4, 10, 10);
            SXSSFCell cell = null;
            for (Member member : students) {
                col_index = 0;
                row = sheet.createRow(++row_index);

                //STT
                cell = row.createCell(col_index);
                cell.setCellValue(row_index - 1);

                //M??
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getCode()) ? member.getCode() : "");

                //H??? v?? t??n
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getName()) ? member.getName() : "");

                //S??T
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getPhone_number()) ? member.getPhone_number() : "");

                //Email
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getEmail()) ? member.getEmail() : "");

                //Ng??y sinh
                cell = row.createCell(++col_index);
                cell.setCellValue(member.getDob() != null ? new SimpleDateFormat("dd/MM/yyyy").format(new Date(member.getDob())) : "_");

                //Gi???i t??nh
                cell = row.createCell(++col_index);
                cell.setCellValue("female".equals(member.getGender()) ? "N???" : "male".equals(member.getGender()) ? "Nam" : "Kh??c");

                //Tr???ng th??i
                cell = row.createCell(++col_index);
                cell.setCellValue(Member.MemberStatus.ACTIVE.equals(member.getStatus()) ? "Ho???t ?????ng" : "Kh??a");

                //?????a ch???
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getAddress()) ? member.getAddress() : "");
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void createTemplateExportTeacher(String pathName, SXSSFWorkbook workbook, List<Member> students) {
        try {
            if (org.apache.commons.collections4.CollectionUtils.isEmpty(students)) return;
            SXSSFSheet sheet = workbook.createSheet(pathName);
            int col_index = 0;
            int row_index = 0;

            SXSSFRow row = sheet.createRow(row_index);

            this.setCellHeader(row, this.createCellStyle(workbook), "STT", col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "M?? Gi???ng vi??n", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "H??? v?? t??n", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "S??T", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Email", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Ng??y sinh", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Gi???i t??nh", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Tr???ng th??i", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "C??c kh??a h???c c?? th??? d???y", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "?????a ch???", ++col_index);

            List<CommandGetAllCourse> courses = courseApplication.getAll().orElse(new ArrayList<>());
            Map<String, String> mapCourse = new HashMap<>();
            for (CommandGetAllCourse item : courses) {
                mapCourse.put(item.get_id(), item.getName());
            }

            this.setColumnWidth(sheet, 2, 5, 7, 4, 7, 4, 4, 4, 10, 10);
            SXSSFCell cell = null;
            for (Member member : students) {
                col_index = 0;
                row = sheet.createRow(++row_index);

                //STT
                cell = row.createCell(col_index);
                cell.setCellValue(row_index - 1);

                //M??
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getCode()) ? member.getCode() : "");

                //H??? v?? t??n
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getName()) ? member.getName() : "");

                //S??T
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getPhone_number()) ? member.getPhone_number() : "");

                //Email
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getEmail()) ? member.getEmail() : "");

                //Ng??y sinh
                cell = row.createCell(++col_index);
                cell.setCellValue(member.getDob() != null ? new SimpleDateFormat("dd/MM/yyyy").format(new Date(member.getDob())) : "_");

                //Gi???i t??nh
                cell = row.createCell(++col_index);
                cell.setCellValue("female".equals(member.getGender()) ? "N???" : "male".equals(member.getGender()) ? "Nam" : "Kh??c");

                //Tr???ng th??i
                cell = row.createCell(++col_index);
                cell.setCellValue(Member.MemberStatus.ACTIVE.equals(member.getStatus()) ? "Ho???t ?????ng" : "Kh??a");

                //C??c kh??a h???c c?? th??? d???y
                String value = "";
                if (!CollectionUtils.isEmpty(member.getCourse_ids())) {
                    for (String item : member.getCourse_ids()) {
                        value += mapCourse.get(item) + ", ";
                    }
                    value = value.substring(0, value.length() - 3);
                }
                if (!CollectionUtils.isEmpty(member.getCourse_ids()))
                    cell = row.createCell(++col_index);
                cell.setCellValue(value);

                //?????a ch???
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getAddress()) ? member.getAddress() : "");
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void createTemplateExportStudent(String pathName, SXSSFWorkbook workbook, List<Member> students) {
        try {
            if (org.apache.commons.collections4.CollectionUtils.isEmpty(students)) return;
            SXSSFSheet sheet = workbook.createSheet(pathName);
            sheet.addMergedRegion(new CellRangeAddress(0, 1, 0, 0));
            sheet.addMergedRegion(new CellRangeAddress(0, 1, 1, 1));
            sheet.addMergedRegion(new CellRangeAddress(0, 1, 2, 2));
            sheet.addMergedRegion(new CellRangeAddress(0, 1, 3, 3));
            sheet.addMergedRegion(new CellRangeAddress(0, 1, 4, 4));
            sheet.addMergedRegion(new CellRangeAddress(0, 1, 5, 5));
            sheet.addMergedRegion(new CellRangeAddress(0, 1, 6, 6));
            sheet.addMergedRegion(new CellRangeAddress(0, 1, 7, 7));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 8, 10));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 11, 13));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 14, 16));
            sheet.addMergedRegion(new CellRangeAddress(0, 1, 17, 17));
            sheet.addMergedRegion(new CellRangeAddress(0, 1, 18, 18));
            sheet.addMergedRegion(new CellRangeAddress(0, 1, 19, 19));
            int col_index = 0;
            int row_index = 0;

            SXSSFRow row = sheet.createRow(row_index);

            this.setCellHeader(row, this.createCellStyle(workbook), "STT", col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "M?? h???c vi??n", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "H??? v?? t??n", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Nickname", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Email", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Ng??y sinh", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Gi???i t??nh", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Tr???ng th??i", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "??i???m ?????u v??o", ++col_index);
            col_index += 2;
            this.setCellHeader(row, this.createCellStyle(workbook), "??i???m hi???n t???i", ++col_index);
            col_index += 2;
            this.setCellHeader(row, this.createCellStyle(workbook), "Ng?????i gi??m h???", ++col_index);
            col_index += 2;
            this.setCellHeader(row, this.createCellStyle(workbook), "?????a ch???", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Ghi ch??", ++col_index);
            row = sheet.createRow(++row_index);
            col_index = 8;
            this.setCellHeader(row, this.createCellStyle(workbook), "?????c", col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Nghe", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "T???ng", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "?????c", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Nghe", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "T???ng", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "T??n", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "S??T", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "Quan h???", ++col_index);
            this.setCellHeader(row, this.createCellStyle(workbook), "S??T", ++col_index);

            this.setColumnWidth(sheet, 2, 5, 7, 4, 7, 4, 3, 3, 3, 3, 3, 3, 3, 3, 7, 4, 4, 7, 7, 4);
            SXSSFCell cell = null;
            for (Member member : students) {
                col_index = 0;
                row = sheet.createRow(++row_index);

                //STT
                cell = row.createCell(col_index);
                cell.setCellValue(row_index - 1);

                //M??
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getCode()) ? member.getCode() : "");

                //H??? v?? t??n
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getName()) ? member.getName() : "");

                //Nickname
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getNick_name()) ? member.getNick_name() : "");

                //Email
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getEmail()) ? member.getEmail() : "");

                //Ng??y sinh
                cell = row.createCell(++col_index);
                cell.setCellValue(member.getDob() != null ? new SimpleDateFormat("dd/MM/yyyy").format(new Date(member.getDob())) : "_");

                //Gi???i t??nh
                cell = row.createCell(++col_index);
                cell.setCellValue("female".equals(member.getGender()) ? "N???" : "male".equals(member.getGender()) ? "Nam" : "Kh??c");

                //Tr???ng th??i
                cell = row.createCell(++col_index);
                cell.setCellValue(Member.MemberStatus.ACTIVE.equals(member.getStatus()) ? "Ho???t ?????ng" : "Kh??a");

                //??i???m ?????u v??o
                //?????c
                cell = row.createCell(++col_index);
                cell.setCellValue(member.getInput_score().getRead());

                //Nghe
                cell = row.createCell(++col_index);
                cell.setCellValue(member.getInput_score().getListen());

                //T???ng
                cell = row.createCell(++col_index);
                cell.setCellValue(member.getInput_score().getTotal());

                //??i???m hi???n t???i
                //?????c
                cell = row.createCell(++col_index);
                cell.setCellValue(member.getCurrent_score().getRead());

                //Nghe
                cell = row.createCell(++col_index);
                cell.setCellValue(member.getCurrent_score().getListen());

                //T???ng
                cell = row.createCell(++col_index);
                cell.setCellValue(member.getCurrent_score().getTotal());

                //Ng?????i gi??m h???
                //T??n
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getGuardian().getName()) ? member.getGuardian().getName() : "");

                //S??T
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getGuardian().getPhone_number()) ? member.getGuardian().getPhone_number() : "");

                //Quan h???
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getGuardian().getRelationship()) ? member.getGuardian().getRelationship() : "");

                //?????a ch???
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getAddress()) ? member.getAddress() : "");

                //Ghi ch??
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getNote()) ? member.getNote() : "");

                //S??T
                cell = row.createCell(++col_index);
                cell.setCellValue(StringUtils.isNotBlank(member.getPhone_number()) ? member.getPhone_number() : "");
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Override
    public Optional<Boolean> updateScoreByExcel(CommandUpdateScoreByExcel command) throws Exception {
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        updateScoreAsync.consumerUpdateScore(command);
        return Optional.of(Boolean.TRUE);
    }

    @Override
    public Optional<List<CommandGetAllTeacher>> getAll(CommandSearchMember command) throws Exception {
        Map<String, Object> query = new HashMap<>();
        query.put("is_deleted", false);
        if (!CollectionUtils.isEmpty(command.getTypes())) {
            query.put("type", new Document("$in", command.getTypes()));
        }
        Map<String, Object> sort = new HashMap<>();
        sort.put("_id", 1);
        List<Member> list = mongoDBConnection.find(query, sort).orElse(new ArrayList<>());
        return Optional.of(list.stream().map(item -> CommandGetAllTeacher.builder()
                ._id(item.get_id().toHexString())
                .name(item.getName())
                .build()).collect(Collectors.toList()));
    }

    private void validateRoleAdminAndReceptionist(String currentRole, List<String> types) throws Exception {
        if (!(Member.MemberType.ADMIN.equals(currentRole)
                || (Member.MemberType.RECEPTIONIST.equals(currentRole)
                && !CollectionUtils.isEmpty(types)
                && types.contains(Member.MemberType.STUDENT)))) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
    }

    @Override
    public Optional<Member> add(CommandAddMember command) throws Exception {
        if (StringUtils.isAnyBlank(command.getName(), command.getEmail(), command.getGender())
                || command.getDob() == null) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        Map<String, Object> query = new HashMap<>();
        query.put("is_deleted", false);
        query.put("email", command.getEmail());
        long count = mongoDBConnection.count(query).orElse(0L);
        if (count > 0) {
            throw new Exception(ExceptionEnum.member_exist);
        }
        if (StringUtils.isNotBlank(command.getPhone_number())) {
            Map<String, Object> query1 = new HashMap<>();
            query1.put("is_deleted", false);
            query1.put("phone_number", command.getPhone_number());
            long count1 = mongoDBConnection.count(query1).orElse(0L);
            if (count1 > 0) {
                throw new Exception(ExceptionEnum.phone_number_used);
            }
        }
        Member member = Member.builder()
                .create_date(System.currentTimeMillis())
                .name(command.getName())
                .email(command.getEmail())
                .type(command.getType() != null ? command.getType() : Member.MemberType.STUDENT)
                .dob(command.getDob())
                .gender(command.getGender())
                .address(command.getAddress())
                .phone_number(command.getPhone_number())
                .nick_name(command.getNick_name())
                .note(command.getNote())
                .guardian(command.getGuardian() == null ? Member.Guardian.builder().build() : command.getGuardian())
                .course_ids(command.getCourse_ids())
                .build();

        String code = codeApplication.generateCodeByType(member.getType());
        member.setCode(code);
        Optional<Member> optional = mongoDBConnection.insert(member);
        if (optional.isPresent()) {
            optional.get().setAvatar("avatar-" + optional.get().get_id().toHexString() + ".png");
            mongoDBConnection.update(optional.get().get_id().toHexString(), optional.get());
            authApplication.add(optional.get());
            return optional;
        }
        return Optional.empty();
    }

    @Override
    public Optional<Member> getByEmail(String email) {
        Map<String, Object> query = new HashMap<>();
        query.put("is_deleted", false);
        query.put("email", email);
        return mongoDBConnection.findOne(query);
    }

    @Override
    public Optional<Member> getByCode(String email) {
        Map<String, Object> query = new HashMap<>();
        query.put("is_deleted", false);
        query.put("code", email);
        return mongoDBConnection.findOne(query);
    }

    @Override
    public Optional<Member> getById(String id) {
        Map<String, Object> query = new HashMap<>();
        query.put("is_deleted", false);
        query.put("_id", new ObjectId(id));
        return mongoDBConnection.findOne(query);
    }

    @Override
    public Optional<Member> update(CommandUpdateMember command) throws Exception {
        if (StringUtils.isAnyBlank(command.getRole(), command.getId(), command.getType())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        try {
            validateRoleAdminAndReceptionist(command.getRole(), Collections.singletonList(command.getType()));
        } catch (Exception e) {
            if (!command.getId().equals(command.getCurrent_member())) {
                throw new Exception(ExceptionEnum.member_type_deny);
            }
        }
        Optional<Member> optional = this.getById(command.getId());
        if (!optional.isPresent()) {
            throw new Exception(ExceptionEnum.member_not_exist);
        }
        Member member = optional.get();
        if (StringUtils.isNotBlank(command.getName())) {
            member.setName(command.getName());
        }
        if (StringUtils.isNotBlank(command.getGender())) {
            member.setGender(command.getGender());
        }
        if (StringUtils.isNotBlank(command.getPhone_number()) && !command.getPhone_number().equals(member.getPhone_number())) {
            Map<String, Object> query1 = new HashMap<>();
            query1.put("is_deleted", false);
            query1.put("phone_number", command.getPhone_number());
            long count1 = mongoDBConnection.count(query1).orElse(0L);
            if (count1 > 0) {
                throw new Exception(ExceptionEnum.phone_number_used);
            }
            member.setPhone_number(command.getPhone_number());
        }
        if (StringUtils.isNotBlank(command.getAddress())) {
            member.setAddress(command.getAddress());
        }
        if (command.getDob() != null) {
            member.setDob(command.getDob());
        }
        if (StringUtils.isNotBlank(command.getNick_name())) {
            member.setNick_name(command.getNick_name());
        }
        if (StringUtils.isNotBlank(command.getNote())) {
            member.setNote(command.getNote());
        }
        if (!CollectionUtils.isEmpty(command.getCourse_ids())) {
            member.setCourse_ids(command.getCourse_ids());
        }
        if (StringUtils.isNotBlank(command.getStatus())) {
            member.setStatus(command.getStatus());
        }
        if (command.getGuardian() != null) {
            member.setGuardian(command.getGuardian());
        }
        return mongoDBConnection.update(member.get_id().toHexString(), member);
    }

    @Override
    public Optional<Boolean> delete(String id, String role) throws Exception {
        if (StringUtils.isAnyBlank(id, role)) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!Member.MemberType.ADMIN.equals(role)) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        return mongoDBConnection.delete(id);
    }

    public void test() {
        Map<String, Object> query = new HashMap<>();
        query.put("email", "heg58147@zwoho.com");
        Member member = mongoDBConnection.findOne(query).get();
        member.setIs_deleted(!member.getIs_deleted());
        mongoDBConnection.update(member.get_id().toHexString(), member);
    }
}
