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

    public static Optional<HintGraph> findById(String hex_string) {
        return findByIdOptional(new ObjectId(hex_string));
    }

    public static void setPolicySubmissionCount(ObjectId graphId, long submissionCount) {
        update(new Document("$set", new Document("policyCount", submissionCount))).where(new Document("_id", graphId));
    }

    public static void registerParsing(ObjectId graphId, long parsedCount, long totalParsingTime) {
        update(new Document("$inc", new Document("parsingTime", 1e-9 * totalParsingTime).append("parsedSubmissionCount", parsedCount))).where(new Document("_id", graphId));
    }

    public static void registerPolicyCalculationTime(ObjectId graphId, long policyCalculationTime) {
        update(new Document("$inc", new Document("policyTime", 1e-9 * policyCalculationTime))).where(new Document("_id", graphId));
    }

    public static void removeAllPolicyStats(ObjectId id) {
        update(new Document("$unset", new Document("policyTime", null).append("policyCount", null))).where("_id", id);
    }

}
