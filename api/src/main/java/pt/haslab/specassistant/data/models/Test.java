package pt.haslab.specassistant.data.models;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.types.ObjectId;


public class Test extends PanacheMongoEntityBase {

    @BsonId
    public ID id;

    public ObjectId graphId;

    public Data data;

    public Test() {
    }

    public Test(ID id) {
        this.id = id;
    }

    public Test setData(Data data) {
        this.data = data;
        return this;
    }

    @BsonIgnore
    public Test setGraphId(ObjectId graph_id) {
        this.graphId = graph_id;
        return this;
    }

    public record ID(String model_id, String type) {
    }

    public record Data(Boolean success, Double time, Integer hintDistance) {
        public Data(Boolean success, Double time) {
            this(success, time, null);
        }

        public Data(Boolean success, Long nano_time) {
            this(success, nano_time * 1e-9, null);
        }

        public Data(Boolean success, Long nano_time, Integer hintDistance) {
            this(success, nano_time * 1e-9, hintDistance);
        }
    }

}
