package com.englishcenter.room.application;

import com.englishcenter.core.utils.MongoDBConnection;
import com.englishcenter.core.utils.Paging;
import com.englishcenter.core.utils.enums.ExceptionEnum;
import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.member.Member;
import com.englishcenter.room.Room;
import com.englishcenter.room.command.CommandAddRoom;
import com.englishcenter.room.command.CommandSearchRoom;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class RoomApplication {
    public final MongoDBConnection<Room> mongoDBConnection;

    @Autowired
    public RoomApplication() {
        mongoDBConnection = new MongoDBConnection<>(MongodbEnum.collection_room, Room.class);
    }

    public Optional<Room> add(CommandAddRoom command) throws Exception {
        if(StringUtils.isAnyBlank(command.getName(), command.getStatus()) || command.getCapacity() == null) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        Room room = Room.builder()
                .name(command.getName())
                .capacity(command.getCapacity())
                .status(command.getStatus())
                .build();
        return mongoDBConnection.insert(room);
    }

    public Optional<Paging<Room>> getList(CommandSearchRoom command) throws Exception {
        Map<String, Object> query = new HashMap<>();
        if (StringUtils.isNotBlank(command.getKeyword())) {
            Map<String, Object> $regex = new HashMap<>();
            $regex.put("$regex", Pattern.compile(command.getKeyword(), Pattern.CASE_INSENSITIVE));
            query.put("name", $regex);
        }
        return mongoDBConnection.find(query, command.getSort(), command.getPage(), command.getSize());
    }

    public Optional<Room> update(CommandAddRoom command) throws Exception {
        if(StringUtils.isBlank(command.getId())) {
            throw new Exception(ExceptionEnum.param_not_null);
        }
        if (!Arrays.asList(Member.MemberType.ADMIN, Member.MemberType.RECEPTIONIST).contains(command.getRole())) {
            throw new Exception(ExceptionEnum.member_type_deny);
        }
        Optional<Room> optional = mongoDBConnection.getById(command.getId());
        if (!optional.isPresent()) {
            throw new Exception(ExceptionEnum.room_not_exist);
        }

        Room room = optional.get();
        if (StringUtils.isNotBlank(command.getName())) {
            room.setName(command.getName());
        }
        if (command.getCapacity() != null) {
            room.setCapacity(command.getCapacity());
        }
        if (StringUtils.isNotBlank(command.getStatus())) {
            room.setStatus(command.getStatus());
        }
        return mongoDBConnection.update(room.get_id().toHexString(), room);
    }

    public Optional<List<Room>> getAll() {
        return mongoDBConnection.find(new HashMap<>());
    }
}
