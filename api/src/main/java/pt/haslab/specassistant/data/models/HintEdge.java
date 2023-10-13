package pt.haslab.specassistant.data.models;


import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

@MongoEntity(collection = "HintEdge")
public class HintEdge extends PanacheMongoEntity {
    public ObjectId graph_id, origin, destination;

    public Float editDistance;

    public Integer count;

    public Boolean policy;


    public static HintEdge createEmpty(ObjectId graph_id, ObjectId originNodeId, ObjectId destinationNodeId) {
        HintEdge result = new HintEdge();
        result.graph_id = graph_id;
        result.origin = originNodeId;
        result.destination = destinationNodeId;

        result.count = 0;

        return result;
    }

    public HintEdge visit() {
        count++;
        return this;
    }

    public Boolean getPolicy() {
        return policy != null && policy;
    }
}
