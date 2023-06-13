package pt.haslab.alloy4fun.data.models;


import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonId;

import java.util.Map;

@MongoEntity(collection = "Singleton")
public class Counter extends PanacheMongoEntityBase {
    @BsonId
    public String key;

    public Long value;

    public Counter() {
    }

    public Counter(String key, Long value) {
        this.key = key;
        this.value = value;
    }

    public static Counter from(Map.Entry<? extends String, ? extends Long> entry) {
        return new Counter(entry.getKey(), entry.getValue());
    }

    public static Long nextGraphId() {
        Counter v = (Counter) findByIdOptional("GraphCounter").orElseGet(() -> new Counter("GraphCounter", 0L));
        try {
            return v.value++;
        } finally {
            v.persistOrUpdate();
        }
    }
}
