package com.englishcenter.member.application;

import com.englishcenter.auth.application.IAuthApplication;
import com.englishcenter.code.CodeApplication;
import com.englishcenter.core.firebase.IFirebaseFileService;
import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.Paging;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.member.Member;
import com.englishcenter.member.command.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
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
        Map<String, Object> query = new HashMap<>();
        query.put("is_deleted", false);
        if (!CollectionUtils.isEmpty(command.getTypes())) {
            query.put("type", new Document("$in", command.getTypes()));
        }
        if (StringUtils.isNotBlank(command.getKeyword())) {
            Map<String, Object> $regex = new HashMap<>();
            $regex.put("$regex", Pattern.compile(command.getKeyword(), Pattern.CASE_INSENSITIVE));
            query.put("name", $regex);
        }
        if (command.getFrom_date() != null && command.getTo_date() != null) {
            query.put("create_date", new Document("$gte", command.getFrom_date()).append("$lte", command.getTo_date()));
        }
        if (!CollectionUtils.isEmpty(command.getGenders())) {
            query.put("gender", new Document("$in", command.getGenders()));
        }
        return mongoDBConnection.find(query, command.getSort(), command.getPage(), command.getSize());
    }

    @Override
    public Optional<Boolean> updateScoreByExel(CommandUpdateScoreByExcel command) throws Exception {
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        URL website = new URL(firebaseFileService.getDownloadUrl(command.getPath(), "imports"));
        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        FileOutputStream fos = new FileOutputStream(command.getPath());
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        rbc.close();

        File _file = new File(command.getPath());
        FileInputStream fis = null;
        Workbook workbook = null;
        try {
            fis = new FileInputStream(_file);
            workbook = WorkbookFactory.create(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (workbook != null) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                Cell cellEmail = row.getCell(0);
                Cell cellRead = row.getCell(1);
                Cell cellListen = row.getCell(2);
                Cell cellType = row.getCell(3);
                try {
                    String email = cellEmail.getStringCellValue();
                    float read = (float) cellRead.getNumericCellValue();
                    float listen = (float) cellListen.getNumericCellValue();
                    String type = cellType.getStringCellValue();
                    Optional<Member> member = getByEmail(email);
                    if (!member.isPresent() || !Member.MemberType.STUDENT.equals(member.get().getType())) {
                        throw new Exception();
                    }
                    Member student = member.get();
                    if ("in".equals(type)) {
                        student.setInput_score(Member.Score.builder()
                                .listen(listen)
                                .read(read)
                                .total(listen + read)
                                .build());
                    } else {
                        student.setCurrent_score(Member.Score.builder()
                                .listen(listen)
                                .read(read)
                                .total(listen + read)
                                .build());
                    }
                    mongoDBConnection.update(student.get_id().toHexString(), student);
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
        }
        workbook.close();
        fis.close();
        _file.delete();
        firebaseFileService.delete("imports/" + command.getPath());
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
                .guardian(command.getGuardian())
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
        if (StringUtils.isNotBlank(command.getPhone_number())) {
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
}
