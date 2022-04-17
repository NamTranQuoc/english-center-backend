package com.englishcenter.log;

import com.englishcenter.core.firebase.FirebaseFileService;
import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.log.command.CommandGetRecent;
import com.englishcenter.member.Member;
import com.englishcenter.member.application.MemberApplication;
import com.englishcenter.member.command.CommandSearchMember;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class LogApplication {
    public final MongoDBConnection<Log> mongoDBConnection;
    @Autowired
    private MemberApplication memberApplication;
    @Autowired
    private FirebaseFileService firebaseFileService;
    @Autowired
    public LogApplication() {
        mongoDBConnection = new MongoDBConnection<>(MongodbEnum.collection_log, Log.class);
    }

    public Optional<List<CommandGetRecent>> getRecent() {
        List<Log> list = mongoDBConnection.findList(new HashMap<>(), CommandSearchMember.Sort.builder()
                .field("_id")
                .is_asc(false)
                .build(), 1, 8).orElse(new ArrayList<>());
        List<ObjectId> ids = list.stream().map(item -> new ObjectId(item.getPerform_by())).collect(Collectors.toList());
        Map<String, Object> query = new HashMap<>();
        query.put("_id", new Document("$in", ids));
        Map<String, Member> listMember = new HashMap<>();
        List<Member> members = memberApplication.mongoDBConnection.find(query).orElse(new ArrayList<>());
        for (Member member : members) {
            listMember.put(member.get_id().toHexString(), member);
        }
        return Optional.of(list.stream()
                .map(item -> CommandGetRecent.builder()
                        .name(item.getName())
                        .class_name(item.getClass_name())
                        .action(item.getAction())
                        .perform_name(listMember.get(item.getPerform_by()).getName())
                        .avatar(firebaseFileService.getDownloadUrl(listMember.get(item.getPerform_by()).getAvatar(), "images"))
                        .build())
                .collect(Collectors.toList()));
    }
}
