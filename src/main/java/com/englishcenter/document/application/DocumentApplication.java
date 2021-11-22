package com.englishcenter.document.application;

import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.Paging;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.document.Document;
import com.englishcenter.document.command.CommandAddDocument;
import com.englishcenter.document.command.CommandSearchDocument;
import com.englishcenter.member.Member;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class DocumentApplication {
    public final MongoDBConnection<Document> mongoDBConnection;

    @Autowired
    public DocumentApplication() {
        mongoDBConnection = new MongoDBConnection<>(MongodbEnum.collection_document, Document.class);
    }

    public Optional<Document> add(CommandAddDocument command) throws Exception {
        if(StringUtils.isAnyBlank(command.getName(), command.getType(), command.getPath())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        String extent = command.getPath().split("\\.")[1];
        if (!Arrays.asList("png", "jgp").contains(extent)) {
            throw new Exception(ExceptionEnum.document_extension_not_match);
        }
        Document document = Document.builder()
                .name(command.getName())
                .course_ids(command.getCourse_ids())
                .type(command.getType())
                .build();
        Optional<Document> insert = mongoDBConnection.insert(document);
        if (insert.isPresent()) {
            insert.get().setPath(command.getType() + "-" + insert.get().get_id().toHexString() + "." + extent);
            mongoDBConnection.update(insert.get().get_id().toHexString(), insert.get());
            return insert;
        }
        return Optional.empty();
    }

    public Optional<Paging<Document>> getList(CommandSearchDocument command) throws Exception {
        Map<String, Object> query = new HashMap<>();
        if (StringUtils.isNotBlank(command.getKeyword())) {
            Map<String, Object> $regex = new HashMap<>();
            $regex.put("$regex", Pattern.compile(command.getKeyword(), Pattern.CASE_INSENSITIVE));
            query.put("name", $regex);
        }
        if (!CollectionUtils.isEmpty(command.getTypes())) {
            query.put("type", new org.bson.Document("$in", command.getTypes()));
        }
        if (!CollectionUtils.isEmpty(command.getCourse_ids())) {
            query.put("course_ids", new org.bson.Document("$in", command.getCourse_ids()));
        }
        return mongoDBConnection.find(query, command.getSort(), command.getPage(), command.getSize());
    }

    public Optional<Document> update(CommandAddDocument command) throws Exception {
        if(StringUtils.isBlank(command.getId())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        Optional<Document> optional = mongoDBConnection.getById(command.getId());
        if (!optional.isPresent()) {
            throw new Exception(ExceptionEnum.document_not_exist);
        }
        Document document = optional.get();
        String extent = command.getPath().split("\\.")[1];
        String oldExtent = document.getPath().split("\\.")[1];
        if (!extent.equals(oldExtent)) {
            throw new Exception(ExceptionEnum.document_extension_not_match);
        }
        if (StringUtils.isNotBlank(command.getName())) {
            document.setName(command.getName());
        }
        if (!CollectionUtils.isEmpty(command.getCourse_ids())) {
            document.setCourse_ids(command.getCourse_ids());
        }
        if (StringUtils.isNotBlank(command.getPath())) {
            document.setPath(document.getType() + "-" + document.get_id().toHexString() + "." + extent);
        }
        return mongoDBConnection.update(document.get_id().toHexString(), document);
    }

    public Optional<Document> getById(String id) {
        return mongoDBConnection.getById(id);
    }

    public Optional<List<Document>> getAll() {
        return mongoDBConnection.find(new HashMap<>());
    }

    public Optional<Boolean> delete(String id, String role) throws Exception{
        if (StringUtils.isAnyBlank(id, role)) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(role)) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        return mongoDBConnection.delete(id);
    }
}
