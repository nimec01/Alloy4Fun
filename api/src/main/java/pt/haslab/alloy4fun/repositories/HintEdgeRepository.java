package pt.haslab.alloy4fun.repositories;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.bson.types.ObjectId;
import pt.haslab.alloy4fun.data.models.HintGraph.HintEdge;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class HintEdgeRepository implements PanacheMongoRepository<HintEdge> {

    public Optional<HintEdge> findByOriginAndDestination(ObjectId origin, ObjectId destination) {
        return find(new Document("origin", origin).append("destination", destination)).firstResultOptional();
    }

    public Optional<HintEdge> findBestScoredByOriginNode(ObjectId origin_id) {
        return find(new Document("origin", origin_id).append("score", new Document("$ne", null)), new Document("score", 1)).firstResultOptional();
    }

    public Stream<HintEdge> streamByDestinationNodeIdAndAllScoreGT(ObjectId destination, Double score) {
        return find(new Document("$or", List.of(new Document("score", new Document("$gt", score)), new Document("score", null))).append("destination", destination).append("origin", new Document("$ne", destination))).stream();
    }

    public Stream<HintEdge> streamByOriginId(ObjectId origin) {
        return find(new Document("origin", origin)).stream();
    }

    public Stream<HintEdge> streamByGraphId(Long graphId) {
        return find(new Document("graph_id", graphId)).stream();
    }

    public void deleteByScoreNull(Long graph_id) {
        delete(new Document("score", null).append("graph_id", graph_id));
    }

    public boolean hasEndpoint(ObjectId node_id) {
        return find(new Document("$or", List.of(new Document("origin", node_id), new Document("destination", node_id)))).firstResultOptional().isPresent();
    }
}
