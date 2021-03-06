package com.englishcenter.auth.application;

import com.englishcenter.auth.Auth;
import com.englishcenter.auth.command.CommandChangePassword;
import com.englishcenter.auth.command.CommandJwt;
import com.englishcenter.auth.command.CommandLogin;
import com.englishcenter.auth.command.CommandSignInWithGoogle;
import com.englishcenter.code.CodeApplication;
import com.englishcenter.core.firebase.FirebaseFileService;
import com.englishcenter.core.mail.Mail;
import com.englishcenter.core.mail.MailService;
import com.englishcenter.core.thymeleaf.ThymeleafService;
import com.englishcenter.core.utils.Generate;
import com.englishcenter.core.utils.HashUtils;
import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.member.Member;
import com.englishcenter.member.application.MemberApplication;
import com.google.gson.Gson;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AuthApplication implements IAuthApplication {
    public final MongoDBConnection<Auth> mongoDBConnection;
    private final String JWT_SECRET = "UUhuhdadyh9*&^777687";
    private final long JWT_EXPIRATION = 24 * 60 * 60 * 1000;
    @Autowired
    private MemberApplication memberApplication;
    @Autowired
    private ThymeleafService thymeleafService;
    @Autowired
    private CodeApplication codeApplication;
    @Autowired
    private FirebaseFileService firebaseFileService;

    @Autowired
    private MailService mailService;

    @Autowired
    public AuthApplication() {
        mongoDBConnection = new MongoDBConnection<>(MongodbEnum.collection_auth, Auth.class);
    }

    @Override
    public Optional<Auth> add(Member member) throws Exception {
        String password = Generate.generateCommonLangPassword();
        Auth auth = Auth.builder()
                .member_id(member.get_id().toHexString())
                .password(HashUtils.getPasswordMD5(password))
                .username(member.getEmail())
                .build();
        Map<String, Object> data = new HashMap<>();
        data.put("name", member.getName());
        data.put("username", member.getEmail());
        data.put("password", password);
        mailService.send(Mail.builder()
                .mail_to(member.getEmail())
                .mail_subject("Th?? ch??o m???ng!")
                .mail_content(thymeleafService.getContent("mailNewMember", data))
                .build());
        return mongoDBConnection.insert(auth);
    }

    @Override
    public Boolean checkJwt(String jwt) {
        return this.decodeJwt(jwt).isPresent();
    }

    private Optional<String> generateToken(Auth auth) throws Exception {
        Optional<Member> optional = memberApplication.getById(auth.getMember_id());
        if (!optional.isPresent()) {
            throw new Exception(ExceptionEnum.member_not_exist);
        }
        Member member = optional.get();
        long now = System.currentTimeMillis();
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        CommandJwt commandJwt = CommandJwt.builder()
                .create_date(now)
                .expiration_date(now + JWT_EXPIRATION)
                .member_id(member.get_id().toHexString())
                .role(member.getType())
                .pw(HashUtils.getPasswordMD5(auth.getPassword()))
                .username(auth.getUsername())
                .build();
        return Optional.of(Jwts.builder()
                .setHeader(header)
                .setPayload(new Gson().toJson(commandJwt))
                .signWith(SignatureAlgorithm.HS512, JWT_SECRET)
                .compact());
    }

    @Override
    public Optional<String> login(CommandLogin command) throws Exception {
        if (StringUtils.isAnyBlank(command.getUsername(), command.getPassword())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        Map<String, Object> query = new HashMap<>();
        query.put("$or", Arrays.asList(
                new Document("email", command.getUsername()),
                new Document("code", command.getUsername()),
                new Document("phone_number", command.getUsername())
        ));
        query.put("status", Member.MemberStatus.ACTIVE);
        Optional<Member> member = memberApplication.mongoDBConnection.findOne(query);
        if (!member.isPresent()) {
            throw new Exception(ExceptionEnum.member_not_exist);
        }
        Map<String, Object> queryAuth = new HashMap<>();
        queryAuth.put("username", member.get().getEmail());
        Optional<Auth> auth = mongoDBConnection.findOne(queryAuth);
        if (!auth.isPresent()) {
            throw new Exception(ExceptionEnum.member_not_exist);
        }
        String hashPass = HashUtils.getPasswordMD5(command.getPassword());
        if (!hashPass.equals(auth.get().getPassword())) {
            throw new Exception(ExceptionEnum.password_incorrect);
        }
        return this.generateToken(auth.get());
    }

    @Override
    public Optional<String> signInWithGoogle(CommandSignInWithGoogle command) throws Exception {
        if (StringUtils.isAnyBlank(command.getEmail(), command.getName())) {
            throw new Exception(ExceptionEnum.cannot_connect);
        }
        Map<String, Object> query = new HashMap<>();
        query.put("email", command.getEmail());
//        query.put("status", Member.MemberStatus.ACTIVE);
        Optional<Member> optional = memberApplication.mongoDBConnection.findOne(query);
        Member member = null;
        if (!optional.isPresent()) {
            member = Member.builder()
                    .create_date(System.currentTimeMillis())
                    .name(command.getName())
                    .email(command.getEmail())
                    .type(Member.MemberType.STUDENT)
                    .dob(System.currentTimeMillis())
                    .gender("other")
                    .address("")
                    .phone_number(command.getPhone_number())
                    .nick_name("")
                    .note("")
                    .guardian(Member.Guardian.builder().build())
                    .build();

            String code = codeApplication.generateCodeByType(member.getType());
            member.setCode(code);
            Optional<Member> insert = memberApplication.mongoDBConnection.insert(member);
            if (!insert.isPresent()) {
                throw new Exception(ExceptionEnum.cannot_connect);
            }
            insert.get().setAvatar("avatar-" + insert.get().get_id().toHexString() + ".png");
            memberApplication.mongoDBConnection.update(insert.get().get_id().toHexString(), insert.get());
            this.add(insert.get());
            if (StringUtils.isNotBlank(command.getAvatar())) {
                try {
                    firebaseFileService.saveFromUrl(command.getAvatar(), "images/" + insert.get().getAvatar());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } else {
            member = optional.get();
        }
        if (Member.MemberStatus.BLOCK.equals(member.getStatus())) {
            throw new Exception(ExceptionEnum.member_not_exist);
        }
        Map<String, Object> queryAuth = new HashMap<>();
        queryAuth.put("username", member.getEmail());
        Optional<Auth> auth = mongoDBConnection.findOne(queryAuth);
        if (!auth.isPresent()) {
            throw new Exception(ExceptionEnum.member_not_exist);
        }
        return this.generateToken(auth.get());
    }

    @Override
    public Optional<CommandJwt> decodeJwt(String jwt) {
        try {
            CommandJwt commandJwt = new Gson().fromJson(Jwts.parser().setSigningKey(JWT_SECRET).parse(jwt).getBody().toString(), CommandJwt.class);
            return Optional.of(commandJwt);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Boolean> resetPassword(CommandChangePassword command) throws Exception {
        if (!Member.MemberType.ADMIN.equals(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        if (StringUtils.isAnyBlank(command.getUsername(), command.getNew_password())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        return mongoDBConnection.update(
                new Document("username", command.getUsername()),
                new Document("$set", new Document("password", HashUtils.getPasswordMD5(command.getNew_password()))));
    }

    @Override
    public Optional<Boolean> requestForgetPassword(String email) throws Exception {
        if (StringUtils.isBlank(email)) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        Optional<Member> optional = memberApplication.getByEmail(email);
        if (!optional.isPresent()) {
            throw new Exception(ExceptionEnum.member_not_exist);
        }
        Member member = optional.get();
        long now = System.currentTimeMillis();
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        CommandJwt commandJwt = CommandJwt.builder()
                .create_date(now)
                .expiration_date(now + 900000)
                .member_id(member.get_id().toHexString())
                .role(member.getType())
                .pw(HashUtils.getPasswordMD5(Generate.generateCommonLangPassword()))
                .username(email)
                .build();
        String token = Jwts.builder()
                .setHeader(header)
                .setPayload(new Gson().toJson(commandJwt))
                .signWith(SignatureAlgorithm.HS512, JWT_SECRET)
                .compact();
        Map<String, Object> data = new HashMap<>();
        data.put("name", member.getName());
        data.put("url", String.format("%s%s", "https://englishcenter-2021.web.app/forget_password/", token));
        mailService.send(Mail.builder()
                .mail_to(email)
                .mail_subject("Kh??i ph???c m???t kh???u!")
                .mail_content(thymeleafService.getContent("mailForgetPassword", data))
                .build());
        System.out.println("1");
        return Optional.of(Boolean.TRUE);
    }

    @Override
    public Optional<Boolean> forgetPassword(CommandChangePassword command) throws Exception {
        if (StringUtils.isAnyBlank(command.getCurrent_id(), command.getConfirm_password(), command.getNew_password())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!command.getNew_password().equals(command.getConfirm_password())) {
            throw new Exception(ExceptionEnum.confirm_password_incorrect);
        }
        Map<String, Object> query = new HashMap<>();
        query.put("member_id", command.getCurrent_id());
        return mongoDBConnection.update(query, new Document("$set", new Document("password", HashUtils.getPasswordMD5(command.getNew_password()))));
    }

    @Override
    public Optional<Boolean> forgetPasswordCode(CommandChangePassword command) throws Exception {
        if (StringUtils.isAnyBlank(command.getConfirm_password(), command.getNew_password(), command.getCode(), command.getUsername())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!command.getNew_password().equals(command.getConfirm_password())) {
            throw new Exception(ExceptionEnum.confirm_password_incorrect);
        }
        Map<String, Object> query = new HashMap<>();
        query.put("username", command.getUsername());
        Optional<Auth> optional = mongoDBConnection.findOne(query);
        if (!optional.isPresent()) {
            throw new Exception(ExceptionEnum.member_not_exist);
        }
        Auth auth = optional.get();
        if (!command.getCode().equals(auth.getCode())) {
            throw new Exception(ExceptionEnum.code_incorrect);
        }

        auth.setPassword(HashUtils.getPasswordMD5(command.getNew_password()));

        return Optional.of(mongoDBConnection.update(auth.get_id().toHexString(), auth).isPresent());
    }

    @Override
    public Optional<Boolean> changePassword(CommandChangePassword command) throws Exception {
        if (StringUtils.isAnyBlank(command.getCurrent_id(), command.getConfirm_password(), command.getNew_password(), command.getOld_password())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        Optional<Auth> optional = mongoDBConnection.findOne(new Document("member_id", command.getCurrent_id()));
        if (!optional.isPresent()) {
            throw new Exception(ExceptionEnum.member_not_exist);
        }
        Auth auth = optional.get();
        if (!HashUtils.getPasswordMD5(command.getOld_password()).equals(auth.getPassword())) {
            throw new Exception(ExceptionEnum.password_incorrect);
        }
        if (!command.getNew_password().equals(command.getConfirm_password())) {
            throw new Exception(ExceptionEnum.confirm_password_incorrect);
        }
        auth.setPassword(HashUtils.getPasswordMD5(command.getNew_password()));
        return Optional.of(mongoDBConnection.update(auth.get_id().toHexString(), auth).isPresent());
    }

    @Override
    public Optional<Boolean> requestCodeForgetPassword(String email) throws Exception {
        if (StringUtils.isBlank(email)) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        Map<String, Object> query = new HashMap<>();
        query.put("username", email);
        Optional<Auth> optional = mongoDBConnection.findOne(query);
        if (!optional.isPresent()) {
            throw new Exception(ExceptionEnum.member_not_exist);
        }
        Auth auth = optional.get();
        String code = Generate.generateCode();
        auth.setCode(code);
        mongoDBConnection.update(auth.get_id().toHexString(), auth);


        Map<String, Object> data = new HashMap<>();
        data.put("username", auth.getUsername());
        data.put("code", code);
        mailService.send(Mail.builder()
                .mail_to(email)
                .mail_subject("Kh??i ph???c m???t kh???u!")
                .mail_content(thymeleafService.getContent("mailForgetPasswordByCode", data))
                .build());
        return Optional.of(Boolean.TRUE);
    }

    @Override
    public Optional<Boolean> loginSuccess(String token, String memberId) {
        Optional<Member> optional = memberApplication.getById(memberId);
        if (optional.isPresent()) {
            Member member = optional.get();
            if (CollectionUtils.isEmpty(member.getTokens())) {
                member.setTokens(new ArrayList<>());
            }
            Set<String> tokens = new HashSet<>(member.getTokens());
            tokens.add(token);
            member.setTokens(new ArrayList<>(tokens));

            memberApplication.mongoDBConnection.update(memberId, member);
            return Optional.of(true);
        }
        return Optional.of(false);
    }

    @Override
    public Optional<Boolean> logout(String token, String memberId) {
        Optional<Member> optional = memberApplication.getById(memberId);
        if (optional.isPresent()) {
            Member member = optional.get();
            if (CollectionUtils.isEmpty(member.getTokens())) {
                member.setTokens(new ArrayList<>());
            }
            Set<String> tokens = new HashSet<>(member.getTokens());
            tokens.remove(token);
            member.setTokens(new ArrayList<>(tokens));

            memberApplication.mongoDBConnection.update(memberId, member);
            return Optional.of(true);
        }
        return Optional.of(false);
    }
}
