package pt.haslab.specassistant.data.models;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class Test extends PanacheMongoEntityBase {

    @BsonId
    public String model_id;

    public ObjectId graph_id;

    public Map<String, Data> data;


    public Test() {
    }

    public Test(String model_id, ObjectId graph_id) {
        this.model_id = model_id;
        this.graph_id = graph_id;
        this.data = new HashMap<>();
    }

    public void register(String type, Test.Data _data) {
        data.put(type, _data);
    }

    public Optional<Data> getData(String type) {
        return Optional.ofNullable(data.get(type));
    }


    public static class Data {
        private Boolean success;
        private Long time;

        public Data() {
        }

        public Data(Boolean success, Long time) {
            this.success = success;
            this.time = time;
        }

        public Boolean getSuccess() {
            return success;
        }

        public Long getTime() {
            return time;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
        }

        public void setTime(Long time) {
            this.time = time;
        }
    }
}
