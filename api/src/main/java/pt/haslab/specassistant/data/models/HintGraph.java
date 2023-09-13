package pt.haslab.specassistant.data.models;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Optional;

@MongoEntity(collection = "HintGraph")
public class HintGraph extends PanacheMongoEntity {

    public String name;

    HintGraph() {
    }

    public static HintGraph newGraph(String name) {
        HintGraph result = new HintGraph();
        result.persist();
        if (name != null && !name.isEmpty())
            result.name = name;
        result.update();
        return result;
    }

    public static void registerParsing(ObjectId graphId, String model_id, long parsedCount, long totalParsingTime) {
        String nest = "parsing." + model_id + ".";
        update(new Document("$set", new Document(nest + "time", 1e-9 * totalParsingTime).append(nest + "count", parsedCount))).where(new Document("_id", graphId));
    }

    public static void registerPolicy(ObjectId graphId, long policyCalculationTime, long policyCount) {
        update(new Document("$set", new Document("policy.time", 1e-9 * policyCalculationTime).append("policy.count", policyCount))).where(new Document("_id", graphId));
    }

    public static void removeAllPolicyStats(ObjectId id) {
        update(new Document("$unset", new Document("policyTime", null).append("policyCount", null))).where("_id", id);
    }

}
