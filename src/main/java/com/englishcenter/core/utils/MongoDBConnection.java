package com.englishcenter.core.utils;

import com.englishcenter.core.utils.enums.MongodbEnum;
import com.englishcenter.member.command.CommandSearchMember;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import eu.dozd.mongo.MongoMapper;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;
import org.springframework.util.CollectionUtils;

import java.util.*;

public class MongoDBConnection<T> {
    private final MongoCollection<T> mongoCollection;

    public MongoDBConnection(String name, Class<T> tClass) {
        ConnectionString connectionString = new ConnectionString(MongodbEnum.connection);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();
        MongoClient mongoClient = MongoClients.create(settings);
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(MongoMapper.getProviders()));
        MongoDatabase database = mongoClient.getDatabase(MongodbEnum.database_name).withCodecRegistry(codecRegistry);
        mongoCollection = database.getCollection(name, tClass);
    }

    public Optional<T> getById(String id) {
        try {
            return Optional.ofNullable(mongoCollection.find(new Document("_id", new ObjectId(id))).first());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<List<T>> find(Map<String, Object> query) {
        try {
            return Optional.of(mongoCollection.find(new Document(query)).into(new ArrayList<>()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<List<T>> find(Map<String, Object> query, Map<String, Object> sort) {
        try {
            return Optional.of(mongoCollection.find(new Document(query)).sort(new Document(sort)).into(new ArrayList<>()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<List<T>> find(Map<String, Object> query, int size) {
        try {
            return Optional.of(mongoCollection.find(new Document(query)).skip(0).limit(size).into(new ArrayList<>()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<List<T>> find(Map<String, Object> query, int size, Map<String, Object> sort) {
        try {
            return Optional.of(mongoCollection.find(new Document(query)).sort(new Document(sort)).skip(0).limit(size).into(new ArrayList<>()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<T> findOne(Map<String, Object> query) {
        try {
            List<T> list = mongoCollection.find(new Document(query)).skip(0).limit(1).into(new ArrayList<>());
            if (CollectionUtils.isEmpty(list)) {
                return Optional.empty();
            }
            return Optional.of(list.get(0));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<T> update(String id, T t) {
        try {
            Document data = this.buildQuerySet(t);
            Map<String, Object> query = new HashMap<>();
            query.put("_id", new ObjectId(id));
            return Optional.ofNullable(mongoCollection.findOneAndUpdate(new Document(query), data));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<Boolean> update(Map<String, Object> query, Map<String, Object> data) {
        try {
            return Optional.of(mongoCollection.updateMany(new Document(query), new Document(data)).getModifiedCount() > 0);
        } catch (Exception e) {
            return Optional.of(Boolean.FALSE);
        }
    }

    public Optional<Long> updateMany(Map<String, Object> query, Map<String, Object> data) {
        try {
            return Optional.of(mongoCollection.updateMany(new Document(query), new Document(data)).getModifiedCount());
        } catch (Exception e) {
            return Optional.of(0L);
        }
    }

    public Optional<Boolean> delete(String id) {
        try {
            Map<String, Object> query = new HashMap<>();
            query.put("_id", new ObjectId(id));
            query.put("is_deleted", false);
            return Optional.of(mongoCollection.updateMany(new Document(query), new Document("$set", new Document("is_deleted", true))).getModifiedCount() > 0);
        } catch (Exception e) {
            return Optional.of(Boolean.FALSE);
        }
    }

    public Optional<Boolean> delete(Map<String, Object> query) {
        try {
            return Optional.of(mongoCollection.deleteMany(new Document(query)).getDeletedCount() > 0);
        } catch (Exception e) {
            return Optional.of(Boolean.FALSE);
        }
    }

    public Optional<Long> count(Map<String, Object> query) {
        try {
            return Optional.of(mongoCollection.count(new Document(query)));
        } catch (Exception e) {
            return Optional.of(0L);
        }
    }

    public Optional<T> insert(T t) {
        mongoCollection.insertOne(t);
        return Optional.of(t);
    }

    public Optional<Boolean> insertMany(List<T> t) {
        try {
            mongoCollection.insertMany(t);
            return Optional.of(Boolean.TRUE);
        } catch (Exception e) {
            return Optional.of(Boolean.FALSE);
        }
    }

    public Boolean checkExistByName(String name) {
        try {
            Map<String, Object> query = new HashMap<>();
            query.put("name", name);
            long count = count(query).orElse(0L);
            return count != 0L;
        } catch (Exception e) {
            return Boolean.FALSE;
        }
    }

    public AggregateIterable<Document> aggregate(List<BasicDBObject> basicDBObjects) {
        return mongoCollection.aggregate(new ArrayList<>(basicDBObjects), Document.class).allowDiskUse(true);
    }

    public void drop(Map<String, Object> query) {
        mongoCollection.deleteMany(new Document(query)).getDeletedCount();
    }

    public void drop(String id) {
        Map<String, Object> query = new HashMap<>();
        query.put("_id", new ObjectId(id));
        mongoCollection.deleteMany(new Document(query));
    }

    public Optional<Paging<T>> find(Map<String, Object> query, CommandSearchMember.Sort sort, int page, int size) {
        try {
            long count = mongoCollection.count(new Document(query));
            if (count > 0) {
                List<T> list = mongoCollection.find(new Document(query)).sort(new Document(sort.getField(), sort.getIs_asc() ? 1 : -1)).skip((page - 1) * size).limit(size).into(new ArrayList<>());
                return Optional.of(Paging.<T>builder()
                        .items(list)
                        .total_items(count)
                        .build());
            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            return Optional.of(Paging.<T>builder()
                    .items(new ArrayList<>())
                    .total_items(0L)
                    .build());
        }
    }

    public Optional<List<T>> findList(Map<String, Object> query, CommandSearchMember.Sort sort, int page, int size) {
        try {
            return Optional.of(mongoCollection.find(new Document(query)).sort(new Document(sort.getField(), sort.getIs_asc() ? 1 : -1)).skip((page - 1) * size).limit(size).into(new ArrayList<>()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Document buildQuerySet(T t) {
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
        };
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> data = objectMapper.convertValue(t, typeRef);
        Document queryItem = new Document();
        queryItem.putAll(data);
        Document query = new Document();
        queryItem.remove("_id");
        query.put("$set", queryItem);
        return query;
    }
}
