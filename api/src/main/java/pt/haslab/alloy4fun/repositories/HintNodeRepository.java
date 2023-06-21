package pt.haslab.alloy4fun.repositories;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.bson.types.ObjectId;
import pt.haslab.alloy4fun.data.models.HintGraph.HintNode;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class HintNodeRepository implements PanacheMongoRepository<HintNode> {

    private static Document appendFormulaToDocument(Document graph_id, Map<String, String> formula) {
        Document query = graph_id;
        for (Map.Entry<String, String> entry : formula.entrySet()) {
            query = query.append("formula." + entry.getKey(), entry.getValue());
        }
        return query;
    }

    public Optional<HintNode> findByGraphIdAndFormula(Long graph_id, Map<String, String> formula) {
        Document query = appendFormulaToDocument(new Document("graph_id", graph_id), formula);
        return find(query).firstResultOptional();
    }

    public boolean incrementByGraphIdAndFormula(Long graph_id, Map<String, String> formula) {
        Document query = appendFormulaToDocument(new Document("graph_id", graph_id), formula);
        return update(new Document("$inc", new Document("count", 1))).where(query) > 0;
    }

    public Optional<HintNode> findBestByGraphIdAndFormulaIn(Long graph_id, List<Map<String, String>> formulas) {
        return find(new Document("$or", formulas.stream().map(x -> appendFormulaToDocument(new Document("graph_id", graph_id), x)).toList()),new Document("valid",-1).append("visits",-1)).firstResultOptional();
    }

    public void incrementLeaveById(ObjectId oldNodeId) {
        update(new Document("$inc", new Document("leaves", 1))).where(new Document("_id", oldNodeId));
    }

    public Stream<HintNode> streamByGraphIdAndValidTrue(Long graphId) {
        return find(new Document("graph_id", graphId).append("valid", true)).stream();
    }

    public Optional<HintNode> getByMostLeavesAndGraphID(Long graph_id) {
        return find(new Document("graph_id", graph_id), new Document("leaves", -1)).firstResultOptional();
    }

    public Collection<HintNode> findAllByIdIn(Collection<ObjectId> ids) {
        return find(new Document("_id", new Document("$in", ids))).stream().toList();
    }


    public Stream<HintNode> streamByIdIn(Collection<ObjectId> ids) {
        return find(new Document("_id", new Document("$in", ids))).stream();
    }

    public Stream<HintNode> streamByGraphId(Long graph_id) {
        return find(new Document("graph_id", graph_id)).stream();
    }
}
