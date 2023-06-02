package pt.haslab.alloy4fun.repositories;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import pt.haslab.alloy4fun.data.models.HintGraph.HintEdge;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class HintEdgeRepository implements PanacheMongoRepository<HintEdge> {


    public Optional<HintEdge> findByNodeIds(List<String> nodeIds) {
        return find(new Document("endpoints.node_id", new Document("$all", nodeIds))).firstResultOptional();
    }

    public Optional<HintEdge> findByOriginAndDestination(String origin, String destination) {
        return findByNodeIds(List.of(origin, destination));
    }

    public Optional<HintEdge> findBestScoredByNode(String origin_id) {
        return find("endpoints.node_id = ?1 ", origin_id, Sort.by("endpoints.score", Sort.Direction.Descending)).firstResultOptional();
    }


    public Stream<HintEdge> findByNodeIdAndAllScoreLT(String node_id, Double score) {
        return find(new Document("endpoints.score", new Document("$not", new Document("$gte", score))).append("endpoints.node_id", node_id)).stream();
    }
}
