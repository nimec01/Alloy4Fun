package pt.haslab.alloy4fun.repositories;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.bson.types.ObjectId;
import pt.haslab.alloy4fun.data.models.HintGraph.HintEdge;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class HintEdgeRepository implements PanacheMongoRepository<HintEdge> {

    private Document orNodeId(Object value) {
        return new Document("$or", List.of(new Document("origin", value), new Document("destination", value)));
    }

    public Optional<HintEdge> findByOriginAndDestination(ObjectId origin, ObjectId destination) {
        return find(new Document("origin", origin).append("destination", destination)).firstResultOptional();
    }

    public Optional<HintEdge> findBestScoredByOriginNode(ObjectId origin_id) {
        return find("origin = ?1", origin_id, Sort.by("score", Sort.Direction.Descending)).firstResultOptional();
    }

    public Stream<HintEdge> streamByDestinationNodeIdAndAllScoreLT(ObjectId destination, Double score) {
        return find(new Document("score", new Document("$not", new Document("$gte", score))).append("destination", destination)).stream();
    }

    public Stream<HintEdge> streamByOriginId(ObjectId origin) {
        return find(new Document("origin", origin)).stream();
    }

    public Stream<HintEdge> streamByDestinationId(ObjectId origin) {
        return find(new Document("destination", origin)).stream();
    }

    public Stream<HintEdge> streamByDestinationWithBanList(ObjectId origin, Collection<ObjectId> bannedIds) {
        return find(new Document("$and", List.of(new Document("destination", origin), new Document("origin", new Document("$nin", bannedIds)), new Document("destination", new Document("$nin", bannedIds))))).stream();
    }

    public Stream<HintEdge> streamByGraphId(Long graphId) {
        return find(new Document("graph_id", graphId)).stream();
    }
}
