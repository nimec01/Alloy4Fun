package pt.haslab.specassistant.data.models;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;


public class Test extends PanacheMongoEntityBase {

    @BsonId
    public ID id;


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

    public record ID(String model_id, ObjectId graph_id, String type) {
    }

    public record Data(Boolean success, Double time) {
        public Data(Boolean success, Long nano_time) {
            this(success, nano_time * 1e-9);
        }
    }

}
