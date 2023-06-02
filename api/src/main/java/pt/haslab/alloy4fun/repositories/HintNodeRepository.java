package pt.haslab.alloy4fun.repositories;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.bson.types.ObjectId;
import pt.haslab.alloy4fun.data.models.HintGraph.HintNode;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class HintNodeRepository implements PanacheMongoRepository<HintNode> {

    public Optional<HintNode> findByGraphIdAndFormula(Long graph_id, Map<String, String> formula) {
        Document query = new Document("graph_id", graph_id);
        for (Map.Entry<String, String> entry : formula.entrySet()) {
            query = query.append("formula." + entry.getKey(), entry.getValue());
        }
        return find(query).firstResultOptional();
    }

    public void incrementLeaveById(ObjectId oldNodeId) {
        update(new Document("$inc", new Document("leaves", 1))).where(new Document("_id", oldNodeId));
    }

    public Stream<HintNode> streamByGraphIdAndValidTrue(Long graphId) {
        return find("graph_id = ?1 and valid = true").stream();
    }
}
